package jdos.dos;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.drives.*;
import jdos.gui.Main;
import jdos.hardware.*;
import jdos.hardware.qemu.*;
import jdos.ints.Bios;
import jdos.ints.Bios_disk;
import jdos.misc.Cross;
import jdos.misc.Log;
import jdos.misc.Msg;
import jdos.misc.Program;
import jdos.misc.setup.Section;
import jdos.shell.Dos_shell;
import jdos.shell.Shell;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;
import jdos.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

public class Dos_programs {
    private static class MOUNT extends Program {
        static private short ZDRIVE_NUM = 25;
        void ListMounts() {
            StringRef name=new StringRef();/*Bit32u*/LongRef size=new LongRef(0);/*Bit16u*/IntRef date=new IntRef(0);/*Bit16u*/IntRef time=new IntRef(0);/*Bit8u*/ShortRef attr=new ShortRef(0);
            /* Command uses dta so set it to our internal dta */
            /*RealPt*/int save_dta = Dos.dos.dta();
            Dos.dos.dta(Dos.dos.tables.tempdta);
            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());

            WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_1"));
            WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_FORMAT"), new Object[] {"Drive","Type","Label"});
            for(int p = 0;p < 8;p++) WriteOut("----------");

            for (int d = 0;d < Dos_files.DOS_DRIVES;d++) {
                if (Dos_files.Drives[d] == null) continue;

                String root = String.valueOf((char)('A'+d))+":\\";
                boolean ret = Dos_files.DOS_FindFirst(root, Dos_system.DOS_ATTR_VOLUME);
                if (ret) {
                    dta.GetResult(name,size,date,time,attr);
                    Dos_files.DOS_FindNext(); //Mark entry as invalid
                } else name.value = "";

                /* Change 8.3 to 11.0 */
                int dot = name.value.indexOf('.');
                if (dot==8) {
                    name.value = name.value.substring(0, 8)+name.value.substring(9);
                }

                WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_FORMAT"),new Object[] {root.substring(0,1), Dos_files.Drives[d].GetInfo(),name});
            }
            Dos.dos.dta(save_dta);
        }
        public void Run() {
            Dos_Drive newdrive=null;char drive='C';
            String label;
            String umount;

            //Hack To allow long commandlines
            ChangeToLongCmd();
            /* Parse the command line */
            /* if the command line is empty show current mounts */
            if (cmd.GetCount()==0) {
                ListMounts();
                return;
            }

            /* In secure mode don't allow people to change mount points.
            * Neither mount nor unmount */
            if(Dosbox.control.SecureMode()) {
                WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                return;
            }

            /* Check for unmounting */
            if ((umount=cmd.FindString("-u",false))!=null) {
                umount = umount.toUpperCase();
                int i_drive = umount.charAt(0)-'A';
                if(i_drive < Dos_files.DOS_DRIVES && i_drive >= 0 && Dos_files.Drives[i_drive]!=null) {
                    switch (DriveManager.UnmountDrive(i_drive)) {
                        case 0:
                            Dos_files.Drives[i_drive] = null;
                            if(i_drive == Dos_files.DOS_GetDefaultDrive())
                                Dos_files.DOS_SetDrive(ZDRIVE_NUM);
                            WriteOut(Msg.get("PROGRAM_MOUNT_UMOUNT_SUCCESS"),new Object[]{umount});
                            break;
                        case 1:
                            WriteOut(Msg.get("PROGRAM_MOUNT_UMOUNT_NO_VIRTUAL"));
                            break;
                        case 2:
                            WriteOut(Msg.get("MSCDEX_ERROR_MULTIPLE_CDROMS"));
                            break;
                    }
                } else {
                    WriteOut(Msg.get("PROGRAM_MOUNT_UMOUNT_NOT_MOUNTED"),new Object[]{new Character(umount.charAt(0))});
                }
                return;
            }

            /* Check for moving Z: */
            /* Only allowing moving it once. It is merely a convenience added for the wine team */
            String newz;
            if (ZDRIVE_NUM == 25 && (newz=cmd.FindString("-z",false))!=null) {
                newz = newz.toUpperCase();
                int i_newz = newz.charAt(0) - 'A';
                if (i_newz >= 0 && i_newz < Dos_files.DOS_DRIVES-1 && Dos_files.Drives[i_newz]==null) {
                    ZDRIVE_NUM = (short)i_newz;
                    /* remap drives */
                    Dos_files.Drives[i_newz] = Dos_files.Drives[25];
                    Dos_files.Drives[25] = null;
                    Dos_shell fs = (Dos_shell) Shell.first_shell;
                    /* Update environment */
                    StringRef line = new StringRef();
                    if (fs.GetEnvStr("PATH",line)){
                        line.value = StringHelper.replace(line.value, "Z:\\", newz+":\\");
                    }
                    if (line.value.length()==0) line.value = newz+":\\";
                    fs.SetEnv("PATH",line.value);
                    fs.SetEnv("COMSPEC",newz+":\\COMMAND.COM");

                    /* Update batch file if running from Z: (very likely: autoexec) */
                    if (fs.bf != null) {
                        if (fs.bf.filename.length()>2 && fs.bf.filename.startsWith("Z:"))
                            fs.bf.filename = newz+fs.bf.filename.substring(1);
                    }
                    /* Change the active drive */
                    if (Dos_files.DOS_GetDefaultDrive() == 25) Dos_files.DOS_SetDrive((short)i_newz);
                }
                return;
            }
            // Show list of cdroms
            if (cmd.FindExist("-cd",false)) {
                File[] roots = File.listRoots();
                WriteOut(Msg.get("PROGRAM_MOUNT_CDROMS_FOUND"),new Object[]{new Integer(roots.length)});
                for (int i=0; i<roots.length; i++) {
                    WriteOut("%2d. %s\n",new Object[]{new Integer(i),roots[i].getAbsolutePath()});
                }
                return;
            }

            String type;
            type = cmd.FindString("-t",true);
            if (type==null || type.length()==0) type = "dir";
            boolean iscdrom = (type.equals("cdrom")); //Used for mscdex bug cdrom label name emulation
            while (true) {
                if (type.equals("floppy") || type.equals("dir") || type.equals("cdrom")) {
                    /*Bit16u*/int[] sizes=new int[4];
                    /*Bit8u*/short mediaid;
                    String str_size="";
                    if (type.equals("floppy")) {
                        str_size="512,1,2880,2880";/* All space free */
                        mediaid=0xF0;		/* Floppy 1.44 media */
                    } else if (type.equals("dir")) {
                        // 512*32*32765==~500MB total size
                        // 512*32*16000==~250MB total free size
                        str_size="512,32,32765,16000";
                        mediaid=0xF8;		/* Hard Disk */
                    } else if (type.equals("cdrom")) {
                        str_size="2048,1,32765,0";
                        mediaid=0xF8;		/* Hard Disk */
                    } else {
                        WriteOut(Msg.get("PROGAM_MOUNT_ILL_TYPE"),new Object[]{type});
                        return;
                    }
                    /* Parse the free space in mb's (kb's for floppies) */
                    String mb_size;
                    if((mb_size=cmd.FindString("-freesize",true))!=null) {
                        /*Bit16u*/int freesize = 0;
                        try {freesize=Integer.parseInt(mb_size);} catch (Exception e){e.printStackTrace();}
                        if (type.equals("floppy")) {
                            // freesize in kb
                            str_size = "512,1,2880,"+String.valueOf(freesize*1024/(512*1));
                        } else {
                            long total_size_cyl=32765;
                            long free_size_cyl=freesize*1024*1024/(512*32);
                            if (free_size_cyl>65534) free_size_cyl=65534;
                            if (total_size_cyl<free_size_cyl) total_size_cyl=free_size_cyl+10;
                            if (total_size_cyl>65534) total_size_cyl=65534;
                            // freesize in mb
                            str_size = "512,32,"+String.valueOf(total_size_cyl)+",65535,"+String.valueOf(free_size_cyl);
                        }
                    }

                    {
                        String s = cmd.FindString("-size",true);
                        if (s!=null && s.length()!=0) str_size = s;
                    }

                    {
                        String[] s = StringHelper.split(str_size,",");
                        for (int i=0;i<s.length;i++) {
                            sizes[i]=0;
                            try {
                                sizes[i] = Integer.parseInt(s[i]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // get the drive letter

                    temp_line=cmd.FindCommand(1);
                    if (temp_line==null || (temp_line.length() > 2) || ((temp_line.length()>1) && (temp_line.charAt(1)!=':'))) break;
                    drive=temp_line.toUpperCase().charAt(0);
                    if (drive<'A' || drive>'Z') break;

                    temp_line=cmd.FindCommand(2);
                    if (temp_line==null) break;
                    if (temp_line.length()==0) break;
                    temp_line = FileHelper.resolve_path(temp_line);
                    File temp_file = new File(temp_line);
                    if (!temp_file.exists()) {
                        WriteOut(Msg.get("PROGRAM_MOUNT_ERROR_1"),new Object[]{temp_line});
                        return;
                    }
                    /* Not a switch so a normal directory/file */
                    if (!temp_file.isDirectory()) {
                        WriteOut(Msg.get("PROGRAM_MOUNT_ERROR_2"),new Object[]{temp_line});
                        return;
                    }

//                    if (temp_line[temp_line.size()-1]!=CROSS_FILESPLIT) temp_line+=CROSS_FILESPLIT;
                    /*Bit8u*/int bit8size=sizes[1];
                    if (type.equals("cdrom")) {
                        int num = -1;
                        Integer tmp_num = cmd.FindInt("-usecd",true);
                        if (tmp_num != null) {
                            num = tmp_num.intValue();
                        }
                        IntRef error = new IntRef(0);
                        if (cmd.FindExist("-aspi",false)) {
                            WriteOut("Direct CDRom support not supported in Java.  Will use local director access");
                            //MSCDEX_SetCDInterface(CDROM_USE_ASPI, num);
                        } else if (cmd.FindExist("-ioctl_dio",false)) {
                            WriteOut("Direct CDRom support not supported in Java.  Will use local director access");
                            //MSCDEX_SetCDInterface(CDROM_USE_IOCTL_DIO, num);
                        } else if (cmd.FindExist("-ioctl_dx",false)) {
                            WriteOut("Direct CDRom support not supported in Java.  Will use local director access");
                            //MSCDEX_SetCDInterface(CDROM_USE_IOCTL_DX, num);
                        } else if (cmd.FindExist("-ioctl_mci",false)) {
                            WriteOut("Direct CDRom support not supported in Java.  Will use local director access");
                            //MSCDEX_SetCDInterface(CDROM_USE_IOCTL_MCI, num);
                        } else if (cmd.FindExist("-noioctl",false)) {
                            WriteOut("Direct CDRom support not supported in Java.  Will use local director access");
                            //MSCDEX_SetCDInterface(CDROM_USE_SDL, num);
                        }
//                        else {
//        #if defined (WIN32)
//                            // Check OS
//                            OSVERSIONINFO osi;
//                            osi.dwOSVersionInfoSize = sizeof(osi);
//                            GetVersionEx(&osi);
//                            if ((osi.dwPlatformId==VER_PLATFORM_WIN32_NT) && (osi.dwMajorVersion>5)) {
//                                // Vista/above
//                                MSCDEX_SetCDInterface(CDROM_USE_IOCTL_DX, num);
//                            } else {
//                                MSCDEX_SetCDInterface(CDROM_USE_IOCTL_DIO, num);
//                            }
//        #else
//                            MSCDEX_SetCDInterface(CDROM_USE_IOCTL_DIO, num);
//        #endif
//                        }
                        newdrive  = new Drive_local_cdrom(drive,temp_line,sizes[0],(short)bit8size,sizes[2],0,mediaid,error);
                        // Check Mscdex, if it worked out...
                        switch (error.value) {
                            case 0  :	WriteOut(Msg.get("MSCDEX_SUCCESS"));				break;
                            case 1  :	WriteOut(Msg.get("MSCDEX_ERROR_MULTIPLE_CDROMS"));	break;
                            case 2  :	WriteOut(Msg.get("MSCDEX_ERROR_NOT_SUPPORTED"));	break;
                            case 3  :	WriteOut(Msg.get("MSCDEX_ERROR_PATH"));				break;
                            case 4  :	WriteOut(Msg.get("MSCDEX_TOO_MANY_DRIVES"));		break;
                            case 5  :	WriteOut(Msg.get("MSCDEX_LIMITED_SUPPORT"));		break;
                            default :	WriteOut(Msg.get("MSCDEX_UNKNOWN_ERROR"));			break;
                        }
                        if (error.value!=0 && error.value!=5) {
                            return;
                        }
                    } else {
                        /* Give a warning when mount c:\ or the / */
                        if( temp_line.equals("c:\\") || temp_line.equals("C:\\") ||
                            temp_line.equals("c:/") || temp_line.equals("C:/")    )
                            WriteOut(Msg.get("PROGRAM_MOUNT_WARNING_WIN"));
                        if(temp_line.equals("/")) WriteOut(Msg.get("PROGRAM_MOUNT_WARNING_OTHER"));
                        if (!temp_line.endsWith("\\") && !temp_line.endsWith("/")) temp_line+=File.separator;
                        newdrive=new Drive_local(temp_line,sizes[0],(short)bit8size,sizes[2],sizes[3],mediaid);                        
                    }
                } else {
                    WriteOut(Msg.get("PROGRAM_MOUNT_ILL_TYPE"),new Object[]{type});
                    return;
                }
                if (Dos_files.Drives[drive-'A']!=null) {
                    WriteOut(Msg.get("PROGRAM_MOUNT_ALREADY_MOUNTED"),new Object[]{new Character(drive),Dos_files.Drives[drive-'A'].GetInfo()});
                    return;
                }
                if (newdrive==null) Log.exit("DOS:Can't create drive");
                Dos_files.Drives[drive-'A']=newdrive;
                /* Set the correct media byte in the table */
                Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.mediaid)+(drive-'A')*2,newdrive.GetMediaByte());
                WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_2"),new Object[]{new Character(drive),newdrive.GetInfo()});
                /* check if volume label is given and don't allow it to updated in the future */
                if ((label=cmd.FindString("-label",true))!=null) newdrive.dirCache.SetLabel(label,iscdrom,false);
                    /* For hard drives set the label to DRIVELETTER_Drive.
          * For floppy drives set the label to DRIVELETTER_Floppy.
          * This way every drive except cdroms should get a label.*/
                else if(type.equals("dir")) {
                    label = drive+"_DRIVE";
                    newdrive.dirCache.SetLabel(label,iscdrom,true);
                } else if(type.equals("floppy")) {
                    label = drive + "_FLOPPY";
                    newdrive.dirCache.SetLabel(label,iscdrom,true);
                }
                if(type.equals("floppy")) Bios_disk.incrementFDD();
                return;
            }
            WriteOut(Msg.get("PROGRAM_MOUNT_USAGE"),new Object[]{"d:\\dosprogs","d:\\dosprogs"});
            WriteOut(Msg.get("PROGRAM_MOUNT_USAGE"),new Object[]{"~/dosprogs","~/dosprogs"});
        }
    }

    static private Program.PROGRAMS_Main MOUNT_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new MOUNT();
        }
    };

    private static class MEM extends Program {
        public void Run() {
            /* Show conventional Memory */
            WriteOut("\n");

            /*Bit16u*/int umb_start=Dos.dos_infoblock.GetStartOfUMBChain();
            /*Bit8u*/short umb_flag=Dos.dos_infoblock.GetUMBChainState();
            /*Bit8u*/int old_memstrat=Dos_memory.DOS_GetMemAllocStrategy()&0xff;
            if (umb_start!=0xffff) {
                if ((umb_flag&1)==1) Dos_memory.DOS_LinkUMBsToMemChain(0);
                Dos_memory.DOS_SetMemAllocStrategy(0);
            }

            /*Bit16u*/IntRef seg=new IntRef(0),blocks=new IntRef(0xffff);
            Dos_memory.DOS_AllocateMemory(seg,blocks);
            if ((Dosbox.machine== MachineType.MCH_PCJR) && (Memory.real_readb(0x2000,0)==0x5a) && (Memory.real_readw(0x2000,1)==0) && (Memory.real_readw(0x2000,3)==0x7ffe)) {
                WriteOut(Msg.get("PROGRAM_MEM_CONVEN"),new Object[]{new Integer(0x7ffe*16/1024)});
            } else WriteOut(Msg.get("PROGRAM_MEM_CONVEN"),new Object[]{new Integer(blocks.value*16/1024)});

            if (umb_start!=0xffff) {
                Dos_memory.DOS_LinkUMBsToMemChain(1);
                Dos_memory.DOS_SetMemAllocStrategy(0x40);	// search in UMBs only

                /*Bit16u*/int largest_block=0,total_blocks=0,block_count=0;
                for (;; block_count++) {
                    blocks.value=0xffff;
                    Dos_memory.DOS_AllocateMemory(seg,blocks);
                    if (blocks.value==0) break;
                    total_blocks+=blocks.value;
                    if (blocks.value>largest_block) largest_block=blocks.value;
                    Dos_memory.DOS_AllocateMemory(seg,blocks);
                }

                /*Bit8u*/short current_umb_flag=Dos.dos_infoblock.GetUMBChainState();
                if ((current_umb_flag&1)!=(umb_flag&1)) Dos_memory.DOS_LinkUMBsToMemChain(umb_flag);
                Dos_memory.DOS_SetMemAllocStrategy(old_memstrat);	// restore strategy

                if (block_count>0) WriteOut(Msg.get("PROGRAM_MEM_UPPER"),new Object[]{new Integer(total_blocks*16/1024),new Integer(block_count),new Integer(largest_block*16/1024)});
            }

            /* Test for and show free XMS */
            CPU_Regs.reg_eax.word(0x4300);Callback.CALLBACK_RunRealInt(0x2f);
            if (CPU_Regs.reg_eax.low()==0x80) {
                CPU_Regs.reg_eax.word(0x4310);Callback.CALLBACK_RunRealInt(0x2f);
                /*Bit16u*/int xms_seg= (int)CPU_Regs.reg_esVal.dword;/*Bit16u*/int xms_off=CPU_Regs.reg_ebx.word();
                CPU_Regs.reg_eax.high(8);
                Callback.CALLBACK_RunRealFar(xms_seg,xms_off);
                if (CPU_Regs.reg_ebx.low()==0) {
                    WriteOut(Msg.get("PROGRAM_MEM_EXTEND"),new Object[]{new Long(CPU_Regs.reg_edx.word())});
                }
            }
            /* Test for and show free EMS */
            /*Bit16u*/IntRef handle=new IntRef(0);
            String emm = "EMMXXXX0";
            if (Dos_files.DOS_OpenFile(emm,0,handle)) {
                Dos_files.DOS_CloseFile(handle.value);
                CPU_Regs.reg_eax.high(0x42);
                Callback.CALLBACK_RunRealInt(0x67);
                WriteOut(Msg.get("PROGRAM_MEM_EXPAND"),new Object[]{new Long(CPU_Regs.reg_ebx.word()*16)});
            }
        }
    }

    static public class RebootException extends RuntimeException {}

    static private Program.PROGRAMS_Main REBOOT_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            throw new RebootException();
        }
    };
    
    static private Program.PROGRAMS_Main MEM_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new MEM();
        }
    };

//    extern Bit32u floppytype;
//
//
    private static class BOOT extends Program {
        private FileIO getFSFile_mounted(String filename, LongRef ksize, LongRef bsize, BooleanRef error) {
            //if return NULL then put in error the errormessage code if an error was requested
            boolean tryload = error.value;
            error.value = false;
            ShortRef drive = new ShortRef();
            FileIO tmpfile;
            StringRef fullname = new StringRef();

            Drive_local ldp;
            if (!Dos_files.DOS_MakeName(filename,fullname,drive)) return null;

            try {
                if (!(Dos_files.Drives[drive.value] instanceof Drive_local)) return null;
                ldp = (Drive_local)Dos_files.Drives[drive.value];

                tmpfile = ldp.GetSystemFilePtr(fullname.value, "rb");
                if(tmpfile == null) {
                    if (!tryload) error.value=true;
                    return null;
                }

                // get file size
                bsize.value = tmpfile.length();
                ksize.value = bsize.value / 1024;
                tmpfile.close();

                tmpfile = ldp.GetSystemFilePtr(fullname.value, "rb+");
                if(tmpfile == null) {
//				if (!tryload) *error=2;
//				return NULL;
                    WriteOut(Msg.get("PROGRAM_BOOT_WRITE_PROTECTED"));
                    tmpfile = ldp.GetSystemFilePtr(fullname.value, "rb");
                    if(tmpfile == null) {
                        if (!tryload) error.value=true;
                        return null;
                    }
                }
                return tmpfile;
            } catch(Exception e) {
                return null;
            }
        }

        FileIO getFSFile(String filename, LongRef ksize, LongRef bsize) {
            return getFSFile(filename, ksize, bsize, false);
        }

        FileIO getFSFile(String filename, LongRef ksize, LongRef bsize,boolean tryload/*=false*/) {
            BooleanRef error = new BooleanRef(tryload);
            FileIO tmpfile = getFSFile_mounted(filename,ksize,bsize,error);
            if (tmpfile!=null) return tmpfile;
            //File not found on mounted filesystem. Try regular filesystem
            filename = FileHelper.resolve_path(filename);
            try {
                tmpfile = FileIOFactory.open(filename,FileIOFactory.MODE_READ|FileIOFactory.MODE_WRITE);
            } catch (Exception e) {
                try {
                    tmpfile = FileIOFactory.open(filename,FileIOFactory.MODE_READ);
                    WriteOut(Msg.get("PROGRAM_BOOT_WRITE_PROTECTED"));
                } catch (Exception e1) {
                    WriteOut(Msg.get("PROGRAM_BOOT_NOT_EXIST"));
                    return null;
                }
            }
            try {
                bsize.value = tmpfile.length();
                ksize.value = bsize.value / 1024;
            } catch (Exception e) {
            }
            return tmpfile;
        }

        private void printError() {
            WriteOut(Msg.get("PROGRAM_BOOT_PRINT_ERROR"));
        }

        private void disable_umb_ems_xms() {
            Section dos_sec = Dosbox.control.GetSection("dos");
            dos_sec.ExecuteDestroy(false);
            dos_sec.HandleInputline("umb=false");
            dos_sec.HandleInputline("xms=false");
            dos_sec.HandleInputline("ems=false");
            dos_sec.ExecuteInit(false);
         }

        static private class bootSector {
//            struct entries {
//                Bit8u jump[3];
//                Bit8u oem_name[8];
//                Bit16u bytesect;
//                Bit8u sectclust;
//                Bit16u reserve_sect;
//                Bit8u misc[496];
//            } bootdata;
            byte[] rawdata = new byte[512];
        }
        public void Run() {
            //Hack To allow long commandlines
            ChangeToLongCmd();
            /* In secure mode don't allow people to boot stuff.
             * They might try to corrupt the data on it */
            if(Dosbox.control.SecureMode()) {
                WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                return;
            }

            FileIO usefile_1=null;
            FileIO usefile_2=null;
            int i=0;
            LongRef floppysize = new LongRef(0);
            LongRef rombytesize_1 = new LongRef(0);
            LongRef rombytesize_2 = new LongRef(0);
            char drive = 'A';
            String cart_cmd="";
            String bochs = null;
            if(cmd.GetCount()==0) {
                printError();
                return;
            }
            while(i<cmd.GetCount()) {
                if ((temp_line=cmd.FindCommand(i+1))!=null) {
                    if (temp_line.equalsIgnoreCase("-bochs")) {
                        if ((temp_line=cmd.FindCommand(i+2))!=null) {
                           bochs=temp_line.toUpperCase();
                        }
                        break;
                    }
                    if (temp_line.equalsIgnoreCase("-l")) {
                        /* Specifying drive... next argument then is the drive */
                        i++;
                        if ((temp_line=cmd.FindCommand(i+1))!=null) {
                            drive=temp_line.toUpperCase().charAt(0);
                            if ((drive != 'A') && (drive != 'C') && (drive != 'D')) {
                                printError();
                                return;
                            }

                        } else {
                            printError();
                            return;
                        }
                        i++;
                        continue;
                    }

                    if(temp_line.equalsIgnoreCase("-e")) {
                        /* Command mode for PCJr cartridges */
                        i++;
                        if((temp_line=cmd.FindCommand(i + 1))!=null) {
                            cart_cmd = temp_line.toUpperCase();
                        } else {
                            printError();
                            return;
                        }
                        i++;
                        continue;
                    }

                    WriteOut(Msg.get("PROGRAM_BOOT_IMAGE_OPEN"), new Object[]{temp_line});
                    LongRef rombytesize = new LongRef(0);
                    FileIO usefile = getFSFile(temp_line, floppysize, rombytesize);
                    if (usefile != null) {
                        Bios_disk.diskSwap[i] = new Bios_disk.imageDisk(usefile, temp_line, floppysize.value, false);
                        if (usefile_1==null) {
                            usefile_1=usefile;
                            rombytesize_1=rombytesize;
                        } else {
                            usefile_2=usefile;
                            rombytesize_2=rombytesize;
                        }
                    } else {
                        WriteOut(Msg.get("PROGRAM_BOOT_IMAGE_NOT_OPEN"), new Object[]{temp_line});
                        return;
                    }

                }
                i++;
            }

            Bios_disk.swapPosition = 0;
            Bios_disk.swapInDisks();

            if (bochs != null) {
                if (bochs.startsWith("CD")) {
                    Block.BlockDriverState cdrom = IDE.getFirstCdrom();
                    if (cdrom == null) {
                        WriteOut(Msg.get("PROGRAM_BOOT_UNABLE"), new Object[] {new Character(drive)});
                        return;
                    }
                }
            } else if(Bios_disk.imageDiskList[drive-65]==null) {
                WriteOut(Msg.get("PROGRAM_BOOT_UNABLE"), new Object[] {new Character(drive)});
                return;
            }
            int eip = 0x7c00;
            bootSector bootarea = new bootSector();


            // This is a working progress to get CD booting to work with Dosbox's bios
            /*
            if (cdrom != null) {
                boolean found = false;
                cdrom.drv.bdrv_read(cdrom, 0x11*4, bootarea.rawdata, 0, 1);
                String label = "CD001\001EL TORITO SPECIFICATION";
                if (bootarea.rawdata[0]==0 && new String(bootarea.rawdata, 1, label.length()).equals(label)) {
                    int lba = IDE.readd(bootarea.rawdata, 0x47);
                    cdrom.drv.bdrv_read(cdrom, lba*4, bootarea.rawdata, 0, 1);
                    if (bootarea.rawdata[0] == 1 && bootarea.rawdata[1] == 0 && bootarea.rawdata[0x1E] == 0x55 && bootarea.rawdata[0x1F] == (byte)0xAA && bootarea.rawdata[0x20] == (byte)0x88) {
                        int media = bootarea.rawdata[0x21];
                        if (media == 0) {
                            int bootSegment = IDE.readw(bootarea.rawdata, 0x22);
                            if (bootSegment!=0)
                                eip = bootSegment;
                            lba = IDE.readd(bootarea.rawdata, 0x28);
                            int count = IDE.readw(bootarea.rawdata, 0x26);
                            bootarea.rawdata = new byte[2048*count];
                            cdrom.drv.bdrv_read(cdrom, lba*4, bootarea.rawdata, 0, count);
                            found = true;
                        }
                    }
                }
                if (!found) {
                    WriteOut(Msg.get("PROGRAM_BOOT_UNABLE"), new Object[] {new Character(drive)});
                    return;
                }
            } else */
            if (bochs == null) {
                Bios_disk.imageDiskList[drive-65].Read_Sector(0,0,1,bootarea.rawdata);
            }
            if ((bootarea.rawdata[0]==0x50) && (bootarea.rawdata[1]==0x43) && (bootarea.rawdata[2]==0x6a) && (bootarea.rawdata[3]==0x72)) {
                if (Dosbox.machine!=MachineType.MCH_PCJR) WriteOut(Msg.get("PROGRAM_BOOT_CART_WO_PCJR"));
                else {
                    byte[] rombuf = new byte[65536];
                    int cfound_at=-1;
                    if (cart_cmd.length()>0) {
                        /* read cartridge data into buffer */
                        try {
                            usefile_1.seek(0x200L);
                            usefile_1.read(rombuf, 0, (int)rombytesize_1.value-0x200);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String cmdlist = "";
                        int ct=6;
                        int clen=rombuf[ct] & 0xFF;
                        byte[] buf=new byte[257];
                        if (cart_cmd.equals("?")) {
                            while (clen!=0 && cmdlist.length()<1024) {
                                cmdlist+=" ";
                                cmdlist+=new String(rombuf, ct+1, clen).toUpperCase();
                                ct+=1+clen+3;
                                clen=rombuf[ct] & 0xFF;
                            }
                            if (ct>6) {
                                WriteOut(Msg.get("PROGRAM_BOOT_CART_LIST_CMDS"),new Object[]{cmdlist});
                            } else {
                                WriteOut(Msg.get("PROGRAM_BOOT_CART_NO_CMDS"));
                            }
                            for(int dct=0;dct<Bios_disk.MAX_SWAPPABLE_DISKS;dct++) {
                                if (Bios_disk.diskSwap[dct] != null) {
                                    Bios_disk.diskSwap[dct].close();
                                    Bios_disk.diskSwap[dct] = null;
                                }
                            }
                            //fclose(usefile_1); //delete diskSwap closes the file
                            return;
                        } else {
                            while (clen!=0) {
                                String cmd = new String(rombuf, ct+1, clen).toUpperCase();

                                ct+=1+clen;

                                if (cart_cmd.equals(buf)) {
                                    cfound_at=ct;
                                    break;
                                }

                                ct+=3;
                                clen=rombuf[ct] & 0xFF;
                            }
                            if (cfound_at<=0) {
                                if (ct>6) {
                                    WriteOut(Msg.get("PROGRAM_BOOT_CART_LIST_CMDS"), new Object[] {cmdlist});
                                } else {
                                    WriteOut(Msg.get("PROGRAM_BOOT_CART_NO_CMDS"));
                                }
                                for(int dct=0;dct<Bios_disk.MAX_SWAPPABLE_DISKS;dct++) {
                                    if (Bios_disk.diskSwap[dct] != null) {
                                        Bios_disk.diskSwap[dct].close();
                                        Bios_disk.diskSwap[dct] = null;
                                    }
                                }
                                //fclose(usefile_1); //Delete diskSwap closes the file
                                return;
                            }
                        }
                    }

                    disable_umb_ems_xms();
                    Memory.PreparePCJRCartRom();

                    if (usefile_1==null) return;

                    LongRef sz1 = new LongRef(0);
                    LongRef sz2 = new LongRef(0);
                    FileIO tfile = getFSFile("system.rom", sz1, sz2);

                    if (tfile!=null) {
                        int drd = 0;
                        try {
                            tfile.seek(0x3000L);
                            drd = tfile.read(rombuf, 0, 0xb000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (drd==0xb000) {
                            for(i=0;i<0xb000;i++) Memory.phys_writeb(0xf3000+i,rombuf[i]);
                        }
                        try {tfile.close();} catch (Exception e) {}
                    }

                    if (usefile_2!=null) {
                        try {
                            usefile_2.seek(0);
                            usefile_2.read(rombuf, 0, 0x200);
                            int romseg_pt=new Ptr(rombuf, 0).readw(0x1ce) << 4;

                            /* read cartridge data into buffer */
                            usefile_2.seek(0x200);
                            usefile_2.read(rombuf, 0, (int)rombytesize_2.value-0x200);
                            //fclose(usefile_2); //usefile_2 is in diskSwap structure which should be deleted to close the file

                            /* write cartridge data into ROM */
                            for(i=0;i<rombytesize_2.value-0x200;i++) Memory.phys_writeb(romseg_pt+i,rombuf[i]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    int romseg = 0;
                    try {
                        usefile_1.seek(0);
                        usefile_1.read(rombuf, 0, 0x200);
                        romseg=new Ptr(rombuf, 0).readw(0x1ce);

                        /* read cartridge data into buffer */
                        usefile_1.seek(0x200);
                        usefile_1.read(rombuf, 0, (int)rombytesize_1.value-0x200);

                        //fclose(usefile_1); //usefile_1 is in diskSwap structure which should be deleted to close the file

                        /* write cartridge data into ROM */
                        for(i=0;i<rombytesize_1.value-0x200;i++) Memory.phys_writeb((romseg << 4)+i,rombuf[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Close cardridges
                    for(int dct=0;dct<Bios_disk.MAX_SWAPPABLE_DISKS;dct++) {
                        if (Bios_disk.diskSwap[dct] != null) {
                            Bios_disk.diskSwap[dct].close();
                            Bios_disk.diskSwap[dct] = null;
                        }
                    }

                    if (cart_cmd.length()==0) {
                        int old_int18=Memory.mem_readd(0x60);
                        /* run cartridge setup */
                        CPU_Regs.SegSet16DS(romseg);
                        CPU_Regs.SegSet16ES(romseg);
                        CPU_Regs.SegSet16SS(0x8000);
                        CPU_Regs.reg_esp.dword=0xfffe;
                        Callback.CALLBACK_RunRealFar(romseg,0x0003);

                        int new_int18=Memory.mem_readd(0x60);
                        if (old_int18!=new_int18) {
                            /* boot cartridge (int18) */
                            CPU_Regs.SegSet16CS(Memory.RealSeg(new_int18));
                            CPU_Regs.reg_eip = Memory.RealOff(new_int18);
                        }
                    } else {
                        if (cfound_at>0) {
                            /* run cartridge setup */
                            CPU_Regs.SegSet16DS(Dos.dos.psp());
                            CPU_Regs.SegSet16ES(Dos.dos.psp());
                            Callback.CALLBACK_RunRealFar(romseg,cfound_at);
                        }
                    }
                }
            } else {
                disable_umb_ems_xms();
                Memory.RemoveEMSPageFrame();
                WriteOut(Msg.get("PROGRAM_BOOT_BOOT"), new Object[] {new Character(drive)});

                /* revector some dos-allocated interrupts */
                Memory.real_writed(0,0x01*4,0xf000ff53);
                Memory.real_writed(0,0x03*4,0xf000ff53);

                if (bochs != null) {
                    try {
                        byte[] data = null;
                        try {
                            FileInputStream fis = new FileInputStream("bios.bin");
                            data = new byte[fis.available()];
                            fis.read(data);
                            fis.close();
                        } catch (Exception e) {
                            data = null;
                        }
                        if (data == null) {
                            FileIO fileIO = FileIOFactory.open("jar://bios.bin", FileIOFactory.MODE_READ);
                            data = new byte[(int)fileIO.length()];
                            fileIO.read(data);
                            fileIO.close();
                        }
                        int address = 0x100000 - data.length;
                        for(i=0;i<data.length;i++) RAM.writeb(address + i, data[i]);

                        byte[] videoData = new byte[0x10000];
                        boolean videoBiosFound = false;
                        try {
                            FileInputStream videofis = new FileInputStream("vgabios.bin");
                            videofis.read(videoData);
                            videofis.close();
                            videoBiosFound = true;
                        } catch (Exception e) {
                        }
                        if (!videoBiosFound) {
                            FileIO fileIO = FileIOFactory.open("jar://vgabios.bin", FileIOFactory.MODE_READ);
                            fileIO.read(videoData, 0, (int)fileIO.length());
                            fileIO.close();
                        }
                        address = 0xC0000;
                        for(i=0;i<videoData.length;i++) RAM.writeb(address + i, videoData[i]);
                        if (Dosbox.svgaCard < SVGACards.SVGA_QEMU) {
                            IoHandler.IO_WriteHandler vga_write  = new IoHandler.IO_WriteHandler() {
                                public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
                                    if (port == 0x500 || port == 0x503) {
                                        System.out.print((char)val);
                                    } else if (port == 0x501 || port == 0x502) {
                                        System.out.println("panic in vgabios at line "+val);
                                    }
                                }
                            };
                            new IoHandler.IO_WriteHandleObject().Install(0x500, vga_write, IoHandler.IO_MA);
                            new IoHandler.IO_WriteHandleObject().Install(0x503, vga_write, IoHandler.IO_MA);
                            new IoHandler.IO_WriteHandleObject().Install(0x501, vga_write, IoHandler.IO_MA);
                            new IoHandler.IO_WriteHandleObject().Install(0x502, vga_write, IoHandler.IO_MA);

                            VBE.registerIoPorts();
                        }

                        int endLoadAddress = (int) (0x100000000l - data.length);
                        Memory.MEM_AddROM(endLoadAddress >>> 12, data.length >>> 12, data);
                        //Memory.MEM_AddROM(0xC0, 0x10, videoData);
                        CPU_Regs.reg_eip = 0xFFF0;
                        CPU_Regs.SegSet16CS(0xF000);
                        CPU_Regs.SegSet16DS(0);
                        CPU_Regs.SegSet16ES(0);
                        CPU_Regs.SegSet16SS(0);
                        CPU_Regs.SegSet16FS(0);
                        CPU_Regs.SegSet16GS(0);
                        if (CPU.CPU_ArchitectureType==CPU.CPU_ARCHTYPE_PENTIUM)
                            CPU_Regs.reg_edx.dword = 0x00000513;
                        else if (CPU.CPU_ArchitectureType==CPU.CPU_ARCHTYPE_PENTIUM_PRO)
                            CPU_Regs.reg_edx.dword = 0x00000611;
                        else
                            CPU_Regs.reg_edx.dword = 0x00000402;

                        IoHandler.IO_WriteHandler bios_write  = new IoHandler.IO_WriteHandler() {
                            public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
                                if (port == 0x8900)
                                    throw new Main.KillException();
                                if (port == 0x402 || port == 0x403) {
                                    System.out.print((char)val);
                                } else if (port == 0x401 || port == 0x402) {
                                    System.out.println("panic in rombios.c at line "+val);
                                }
                            }
                        };
                        new IoHandler.IO_WriteHandleObject().Install(0x400, bios_write, IoHandler.IO_MA);
                        new IoHandler.IO_WriteHandleObject().Install(0x401, bios_write, IoHandler.IO_MA);
                        new IoHandler.IO_WriteHandleObject().Install(0x402, bios_write, IoHandler.IO_MA);
                        new IoHandler.IO_WriteHandleObject().Install(0x403, bios_write, IoHandler.IO_MA);
                        new IoHandler.IO_WriteHandleObject().Install(0x8900, bios_write, IoHandler.IO_MA);

                        int equipment = 0x02; /* FPU is there */
                        equipment |= 0x04; /* PS/2 mouse installed */
                        int fdCount = 0;
                        if (Floppy.isDriveReady(0))
                            fdCount++;
                        if (Floppy.isDriveReady(1))
                            fdCount++;
                        if (fdCount == 1) {
                            equipment|=0x01;
                        } else if (fdCount == 2) {
                            equipment|=0x41;
                        }
                        Cmos.CMOS_SetRegister(0x14, (byte)equipment);
                        if (bochs.startsWith("CD")) {
                            Cmos.CMOS_SetRegister(0x3d, (byte)0x3);
                        } else if (bochs.startsWith("FD")) {
                            Cmos.CMOS_SetRegister(0x3d, (byte)0x1);
                        } else {
                            Cmos.CMOS_SetRegister(0x3d, (byte)0x2);
                        }

                        Cmos.CMOS_SetRegister(0x12, (IDE.getDrive(0, 0)!=null ? 0xf0 : 0) | (IDE.getDrive(0, 1)!=null ? 0x0f : 0));
                        cmos_init_hd(0x19, 0x1b, IDE.getDrive(0, 0));
                        cmos_init_hd(0x1a, 0x24, IDE.getDrive(0, 1));

                        int val = 0;
                        for (i = 0; i < 4; i++) {
                            Internal.IDEState s = IDE.getDrive(i/2, i%2);
                            /* NOTE: ide_get_geometry() returns the physical
                               geometry.  It is always such that: 1 <= sects <= 63, 1
                               <= heads <= 16, 1 <= cylinders <= 16383. The BIOS
                               geometry can be different if a translation is done. */
                            if (s != null && s.drive_kind == IDE.IDE_HD && s.chs_trans != Block.BIOS_ATA_TRANSLATION_AUTO) {
                                val |= (s.chs_trans - 1) << (i * 2);
                            }
                        }
                        Cmos.CMOS_SetRegister(0x39, val);
                        //Core_dynamic.CPU_Core_Dynamic_Cache_Init(true);
                        //CPU.cpudecoder= Core_dynamic.CPU_Core_Dynamic_Run;
                        //DecodeBlock.start=1;
                        Bios.boot = true;

                        Floppy.initIO();
                        for (i=0;i<4;i++) {
                            IDEBus controller = IDE.getIDEController(i);
                            if (controller!=null)
                                controller.initIO();
                        }
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
                for(i=0;i<bootarea.rawdata.length;i++) Memory.real_writeb(0, 0x7c00 + i, bootarea.rawdata[i]);

                CPU_Regs.SegSet16CS(0);
                CPU_Regs.reg_eip = eip;
                CPU_Regs.SegSet16DS(0);
                CPU_Regs.SegSet16ES(0);
                /* set up stack at a safe place */
                CPU_Regs.SegSet16SS(0x7000);
                CPU_Regs.reg_esp.dword=0x100;
                CPU_Regs.reg_esi.dword=0;
                CPU_Regs.reg_ecx.dword=1;
                CPU_Regs.reg_ebp.dword=0;
                CPU_Regs.reg_eax.dword=0;
                CPU_Regs.reg_edx.dword=0;

                /*
                if (cdrom!=null) {
                    CPU_Regs.reg_eax.word(0xAA55);
                }
                */
                if (drive>='C')
                    CPU_Regs.reg_edx.low(0x80+drive-'C');
                else
                    CPU_Regs.reg_edx.low(drive-'A');
//                Core_dynamic.CPU_Core_Dynamic_Cache_Init(true);
//                CPU.cpudecoder= Core_dynamic.CPU_Core_Dynamic_Run;
//                DecodeBlock.start = 1;

                CPU_Regs.reg_ebx.dword=0x7c00; //Real code probably uses bx to load the image
            }
        }

        static void cmos_init_hd(int type_ofs, int info_ofs, Internal.IDEState hd)
        {
            if (hd == null || hd.drive_kind != IDE.IDE_HD)
                return;
            int cylinders = hd.cylinders, heads=hd.heads, sectors=hd.sectors;
            Cmos.CMOS_SetRegister(type_ofs, 47);
            Cmos.CMOS_SetRegister(info_ofs, cylinders);
            Cmos.CMOS_SetRegister(info_ofs + 1, cylinders >> 8);
            Cmos.CMOS_SetRegister(info_ofs + 2, heads);
            Cmos.CMOS_SetRegister(info_ofs + 3, 0xff);
            Cmos.CMOS_SetRegister(info_ofs + 4, 0xff);
            Cmos.CMOS_SetRegister(info_ofs + 5, 0xc0 | (((heads > 8)?1:0) << 3));
            Cmos.CMOS_SetRegister(info_ofs + 6, cylinders);
            Cmos.CMOS_SetRegister(info_ofs + 7, cylinders >> 8);
            Cmos.CMOS_SetRegister(info_ofs + 8, sectors);
        }
    }

    static private Program.PROGRAMS_Main BOOT_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new BOOT();
        }
    };

//
//    #if C_DEBUG
//    class LDGFXROM : public Program {
//    public:
//        void Run(void) {
//            if (!(cmd->FindCommand(1, temp_line))) return;
//
//            Bit8u drive;
//            char fullname[DOS_PATHLENGTH];
//
//            localDrive* ldp=0;
//            if (!DOS_MakeName((char *)temp_line.c_str(),fullname,&drive)) return;
//
//            try {
//                ldp=dynamic_cast<localDrive*>(Drives[drive]);
//                if(!ldp) return;
//
//                FILE *tmpfile = ldp->GetSystemFilePtr(fullname, "rb");
//                if(tmpfile == NULL) {
//                    LOG_MSG("BIOS file not accessible.");
//                    return;
//                }
//                fseek(tmpfile, 0L, SEEK_END);
//                if (ftell(tmpfile)>0x10000) {
//                    LOG_MSG("BIOS file too large.");
//                    return;
//                }
//                fseek(tmpfile, 0L, SEEK_SET);
//
//                PhysPt rom_base=PhysMake(0xc000,0);
//
//                Bit8u vga_buffer[0x10000];
//                Bitu data_written=0;
//                Bitu data_read=fread(vga_buffer, 1, 0x10000, tmpfile);
//                for (Bitu ct=0; ct<data_read; ct++) {
//                    phys_writeb(rom_base+(data_written++),vga_buffer[ct]);
//                }
//                fclose(tmpfile);
//
//                rom_base=PhysMake(0xf000,0);
//                phys_writeb(rom_base+0xf065,0xcf);
//            }
//            catch(...) {
//                return;
//            }
//
//            reg_flags&=~FLAG_IF;
//            CALLBACK_RunRealFar(0xc000,0x0003);
//        }
//    };
//
//    static void LDGFXROM_ProgramStart(Program * * make) {
//        *make=new LDGFXROM;
//    }
//    #endif


// LOADFIX

    private static class LOADFIX extends Program {
        public void Run() {
            /*Bit16u*/int commandNr	= 1;
            /*Bit16u*/int kb			= 64;
            if ((temp_line=cmd.FindCommand(commandNr))!=null) {
                if (temp_line.startsWith("-") && temp_line.length()>1) {
                    char ch = temp_line.toUpperCase().charAt(1);
                    if (ch=='D' || ch=='F') {
                        // Deallocate all
                        Dos_memory.DOS_FreeProcessMemory(0x40);
                        WriteOut(Msg.get("PROGRAM_LOADFIX_DEALLOCALL"),new Object[]{new Integer(kb)});
                        return;
                    } else {
                        // Set mem amount to allocate
                        kb = Integer.parseInt(temp_line.substring(1));
                        if (kb==0) kb=64;
                        commandNr++;
                    }
                }
            }
            // Allocate Memory
            /*Bit16u*/IntRef segment=new IntRef(0);
            /*Bit16u*/IntRef blocks = new IntRef(kb*1024/16);
            if (Dos_memory.DOS_AllocateMemory(segment,blocks)) {
                Dos_MCB mcb=new Dos_MCB(segment.value-1);
                mcb.SetPSPSeg(0x40);			// use fake segment
                WriteOut(Msg.get("PROGRAM_LOADFIX_ALLOC"),new Object[]{new Integer(kb)});
                // Prepare commandline...
                if ((temp_line=cmd.FindCommand(commandNr++))!=null) {
                    // get Filename
                    String filename = temp_line;
                    // Setup commandline
                    boolean ok;
                    String args = "";

                    do {
                        ok = (temp_line=cmd.FindCommand(commandNr++))!=null;
                        args+=temp_line;
                        args+=" ";
                    } while (ok);
                    // Use shell to start program
                    Dos_shell shell = new Dos_shell();
                    shell.Execute(filename,args);
                    Dos_memory.DOS_FreeMemory(segment.value);
                    WriteOut(Msg.get("PROGRAM_LOADFIX_DEALLOC"),new Object[]{new Integer(kb)});
                }
            } else {
                WriteOut(Msg.get("PROGRAM_LOADFIX_ERROR"),new Object[]{new Integer(kb)});
            }
        }
    }

    static private Program.PROGRAMS_Main LOADFIX_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new LOADFIX();
        }
    };

// RESCAN

    private static class RESCAN extends Program {
        public void Run() {
            boolean all = false;

            /*Bit8u*/short drive = Dos_files.DOS_GetDefaultDrive();

            if((temp_line=cmd.FindCommand(1))!=null) {
                //-A -All /A /All
                if(temp_line.length() >= 2 && (temp_line.charAt(0) == '-' ||temp_line.charAt(0) =='/')&& (temp_line.charAt(1) == 'a' || temp_line.charAt(1) =='A') ) all = true;
                else if(temp_line.length() == 2 && temp_line.charAt(1) == ':') {
                    drive  = (short)(temp_line.toLowerCase().charAt(0) - 'a');
                }
            }
            // Get current drive
            if (all) {
                for(/*Bitu*/int i =0; i<Dos_files.DOS_DRIVES;i++) {
                    if (Dos_files.Drives[i]!=null) Dos_files.Drives[i].EmptyCache();
                }
                WriteOut(Msg.get("PROGRAM_RESCAN_SUCCESS"));
            } else {
                if (drive < Dos_files.DOS_DRIVES && Dos_files.Drives[drive]!=null) {
                    Dos_files.Drives[drive].EmptyCache();
                    WriteOut(Msg.get("PROGRAM_RESCAN_SUCCESS"));
                }
            }
        }
    }

    static private Program.PROGRAMS_Main RESCAN_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new RESCAN();
        }
    };

    private static class INTRO extends Program {
        void DisplayMount() {
            /* Basic mounting has a version for each operating system.
             * This is done this way so both messages appear in the language file*/
            WriteOut(Msg.get("PROGRAM_INTRO_MOUNT_START"));
    //#if (WIN32)
    //        WriteOut(Msg.get("PROGRAM_INTRO_MOUNT_WINDOWS"));
    //#else
            WriteOut(Msg.get("PROGRAM_INTRO_MOUNT_OTHER"));
    //#endif
            WriteOut(Msg.get("PROGRAM_INTRO_MOUNT_END"));
        }

        public void Run() {
            /* Only run if called from the first shell (Xcom TFTD runs any intro file in the path) */
            if(new Dos_PSP(Dos.dos.psp()).GetParent() != new Dos_PSP(new Dos_PSP(Dos.dos.psp()).GetParent()).GetParent()) return;
            if(cmd.FindExist("cdrom",false)) {
                WriteOut(Msg.get("PROGRAM_INTRO_CDROM"));
                return;
            }
            if(cmd.FindExist("mount",false)) {
                WriteOut("\033[2J");//Clear screen before printing
                DisplayMount();
                return;
            }
            if(cmd.FindExist("special",false)) {
                WriteOut(Msg.get("PROGRAM_INTRO_SPECIAL"));
                return;
            }
            /* Default action is to show all pages */
            WriteOut(Msg.get("PROGRAM_INTRO"));
            /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
            Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
            DisplayMount();
            Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
            WriteOut(Msg.get("PROGRAM_INTRO_CDROM"));
            Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
            WriteOut(Msg.get("PROGRAM_INTRO_SPECIAL"));
        }
    }

    static private Program.PROGRAMS_Main INTRO_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new INTRO();
        }
    };

    private static class IMGMOUNT extends Program {
        public void Run() {
            //Hack To allow long commandlines
            ChangeToLongCmd();
            /* In secure mode don't allow people to change imgmount points.
             * Neither mount nor unmount */
            if (Dosbox.control.SecureMode()) {
                WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                return;
            }
            Dos_Drive newdrive = null;
            Bios_disk.imageDisk newImage = null;
            /*Bit32u*/long imagesize;
            char drive;
            String label;
            Vector paths = new Vector();
            String umount;
            /* Check for unmounting */
            if ((umount=cmd.FindString("-u",false))!=null) {
                int i_drive = umount.toUpperCase().charAt(0)-'A';
                if (i_drive < Dos_files.DOS_DRIVES && i_drive >= 0 && Dos_files.Drives[i_drive]!=null) {
                    switch (DriveManager.UnmountDrive(i_drive)) {
                    case 0:
                        Dos_files.Drives[i_drive] = null;
                        if (i_drive == Dos_files.DOS_GetDefaultDrive())
                            Dos_files.DOS_SetDrive((short)('Z' - 'A'));
                        WriteOut(Msg.get("PROGRAM_MOUNT_UMOUNT_SUCCESS"),new Object[]{new Character(umount.charAt(0))});
                        break;
                    case 1:
                        WriteOut(Msg.get("PROGRAM_MOUNT_UMOUNT_NO_VIRTUAL"));
                        break;
                    case 2:
                        WriteOut(Msg.get("MSCDEX_ERROR_MULTIPLE_CDROMS"));
                        break;
                    }
                } else {
                    WriteOut(Msg.get("PROGRAM_MOUNT_UMOUNT_NOT_MOUNTED"),new Object[]{new Character(umount.charAt(0))});
                }
                return;
            }


            BooleanRef ide_slave = new BooleanRef(false);
		    IntRef ide_index = new IntRef(-1);
		    String ideattach;
            String type;
            String fstype;

            type = cmd.FindString("-t",true);
            if (type == null) type = "hdd";
            else type = type.toLowerCase();

            ideattach = cmd.FindString("-ide",true);
            if (ideattach == null) ideattach = "auto";
            else ideattach = ideattach.toLowerCase();

            fstype = cmd.FindString("-fs",true);
            if (fstype == null) fstype = "fat";

            if(type.equals("cdrom")) type = "iso"; //Tiny hack for people who like to type -t cdrom
            if(type.equals("zip")) fstype = "zip"; // Tiny hack for zip files
            /*Bit8u*/short mediaid;
            if (type.equals("floppy") || type.equals("hdd") || type.equals("iso") || type.equals("zip")) {
                /*Bit16u*/int[] sizes = new int[4];
                boolean imgsizedetect=false;

                String str_size="";
                mediaid=0xF8;

                if (ideattach.equals("auto")) {
                    IDE.IDE_Auto(ide_index, ide_slave);
                    System.out.println("IDE: index "+ide_index.value+" slave="+ide_slave.value);
                } else if (!ideattach.equals("none") && ideattach.length()>0 && Character.isDigit(ideattach.charAt(0))) {
                    ide_index.value = ideattach.charAt(0)-'1';
                    if (ideattach.length()>1 && ideattach.charAt(1)=='s')
                        ide_slave.value = true;
                    System.out.println("IDE: index "+ide_index.value+" slave="+ide_slave.value);
                }

                if (type.equals("floppy")) {
                    mediaid=0xF0;
                } else if (type.equals("iso")) {
                    str_size="2048,1,60000,0"; // ignored, see drive_iso.cpp (AllocationInfo)
                    mediaid=0xF8;
                    fstype = "iso";
                }
                String s = cmd.FindString("-size",true);
                if (s != null)
                    str_size = s;

                if (type.equals("hdd") && str_size.length()==0) {
                    imgsizedetect=true;
                } else {
                    String[] ss = StringHelper.split(str_size, ",");
                    for (int i=0;i<ss.length && i<sizes.length;i++) {
                        try {sizes[i] = Integer.parseInt(ss[i]);} catch (Exception e) {e.printStackTrace();}
                    }
                }

                if (fstype.equals("fat") || fstype.equals("iso") || fstype.equals("zip")) {
                    // get the drive letter
                    if ((temp_line=cmd.FindCommand(1))==null || (temp_line.length() > 2) || ((temp_line.length()>1) && (temp_line.charAt(1)!=':'))) {
                        WriteOut_NoParsing(Msg.get("PROGRAM_IMGMOUNT_SPECIFY_DRIVE"));
                        return;
                    }
                    drive=temp_line.toUpperCase().charAt(0);
                    if (!StringHelper.isalpha(drive)) {
                        WriteOut_NoParsing(Msg.get("PROGRAM_IMGMOUNT_SPECIFY_DRIVE"));
                        return;
                    }
                } else if (fstype.equals("none")) {
                    temp_line = cmd.FindCommand(1);
                    if ((temp_line.length() > 1) || (!StringHelper.isdigit(temp_line.charAt(0)))) {
                        WriteOut_NoParsing(Msg.get("PROGRAM_IMGMOUNT_SPECIFY2"));
                        return;
                    }
                    drive=temp_line.charAt(0);
                    if ((drive<'0') || (drive>3+'0')) {
                        WriteOut_NoParsing(Msg.get("PROGRAM_IMGMOUNT_SPECIFY2"));
                        return;
                    }
                } else {
                    WriteOut(Msg.get("PROGRAM_IMGMOUNT_FORMAT_UNSUPPORTED"),new Object[]{fstype});
                    return;
                }

                // find all file parameters, assuming that all option parameters have been removed
                while((temp_line=cmd.FindCommand(paths.size() + 2))!=null && temp_line.length()>0) {
                    if (FileIOFactory.isRemote(temp_line)) {
                        paths.add(temp_line);
                        continue;
                    }
                    File f = new File(temp_line);
                    if (!f.exists()) {
                        //See if it works if the ~ are written out
                        String homedir=Cross.ResolveHomedir(temp_line);
                        f = new File(homedir);
                        if(f.exists()) {
                            temp_line = homedir;
                        } else {
                            // convert dosbox filename to system filename
                            StringRef fullname = new StringRef();
                            String tmp = temp_line;

                            /*Bit8u*/ShortRef dummy = new ShortRef();
                            if (!Dos_files.DOS_MakeName(tmp, fullname, dummy) || !Dos_files.Drives[dummy.value].GetInfo().startsWith("local directory")) {
                                WriteOut(Msg.get("PROGRAM_IMGMOUNT_NON_LOCAL_DRIVE"));
                                return;
                            }

                            if (!(Dos_files.Drives[dummy.value] instanceof Drive_local)) {
                                WriteOut(Msg.get("PROGRAM_IMGMOUNT_FILE_NOT_FOUND"));
                                return;
                            }
                            Drive_local ldp = (Drive_local)Dos_files.Drives[dummy.value];

                            StringRef t = new StringRef(tmp);
                            ldp.GetSystemFilename(t, fullname.value);
                            temp_line = t.value;
                            if (!new File(temp_line).exists()) {
                                WriteOut(Msg.get("PROGRAM_IMGMOUNT_FILE_NOT_FOUND")+"    "+temp_line);
                                return;
                            }
                        }
                    }
                    if (new File(temp_line).isDirectory()) {
                        WriteOut(Msg.get("PROGRAM_IMGMOUNT_MOUNT"));
                        return;
                    }
                    paths.add(temp_line);
                }
                if (paths.size() == 0) {
                    WriteOut(Msg.get("PROGRAM_IMGMOUNT_SPECIFY_FILE"));
                    return;
                }
                if (paths.size() == 1)
                    temp_line = (String)paths.elementAt(0);
                if (paths.size() > 1 && !fstype.equals("iso")) {
                    WriteOut(Msg.get("PROGRAM_IMGMOUNT_MULTIPLE_NON_CUEISO_FILES"));
                    return;
                }
                
                if (fstype.equals("fat")) {
                    if (imgsizedetect) {
                        FileIO diskfile = null;

                        try {
                            diskfile = FileIOFactory.open(temp_line, FileIOFactory.MODE_READ|FileIOFactory.MODE_WRITE);
                            /*Bit32u*/long fcsize = diskfile.length()/512;
                            /*Bit8u*/byte[] buf=new byte[512];

                            if (diskfile.read(buf)<512) {
                                diskfile.close();
                                WriteOut(Msg.get("PROGRAM_IMGMOUNT_INVALID_IMAGE"));
                                return;
                            }
                            diskfile.close();
                            if ((buf[510]!=0x55) || (buf[511]!=(byte)0xaa)) {
                                WriteOut(Msg.get("PROGRAM_IMGMOUNT_INVALID_GEOMETRY"));
                                return;
                            }

                            // check MBR partition entry 1
                            /*Bitu*/int starthead = buf[0x1bf] & 0xFF;
                            /*Bitu*/int startsect = buf[0x1c0] & 0x3f - 1;
                            /*Bitu*/int startcyl = buf[0x1c1]|((buf[0x1c0]&0xc0)<<2);
                            /*Bitu*/int endcyl = buf[0x1c5]|((buf[0x1c4]&0xc0)<<2);

                            /*Bitu*/int heads = (buf[0x1c3] & 0xFF) +1;
                            /*Bitu*/int sectors = buf[0x1c4] & 0x3f;

                            /*Bitu*/long pe1_size = new IntPtr(buf, 0x1ca).readd(0);
                            boolean yet_detected = false;
                            if(pe1_size!=0) {
                                /*Bitu*/int  part_start = startsect + sectors*starthead + startcyl*sectors*heads;
                                /*Bitu*/int  part_end = heads*sectors*endcyl;
                                /*Bitu*/int  part_len = part_end - part_start;
                                // partition start/end sanity check
                                // partition length should not exceed file length
                                // real partition size can be a few cylinders less than pe1_size
                                // if more than 1023 cylinders see if first partition fits
                                // into 1023, else bail.
                                if((part_len<0)||(part_len > pe1_size)||(pe1_size > fcsize)||
                                    ((pe1_size-part_len)/(sectors*heads)>2)||
                                    ((pe1_size/(heads*sectors))>1023)) {
                                    //LOG_MSG("start(c,h,s) %u,%u,%u",startcyl,starthead,startsect);
                                    //LOG_MSG("endcyl %u heads %u sectors %u",endcyl,heads,sectors);
                                    //LOG_MSG("psize %u start %u end %u",pe1_size,part_start,part_end);
                                } else {
                                    sizes[0]=512; sizes[1]=sectors;
                                    sizes[2]=heads; sizes[3]=(int)(fcsize/(heads*sectors));
                                    if(sizes[3]>1023) sizes[3]=1023;
                                    yet_detected = true;
                                }
                            }
                            if(!yet_detected) {
                                // Try bximage disk geometry
                                /*Bitu*/int cylinders=(int)(fcsize/(16*63));
                                // Int13 only supports up to 1023 cylinders
                                // For mounting unknown images we could go up with the heads to 255
                                if ((cylinders*16*63==fcsize)&&(cylinders<1024)) {
                                    yet_detected=true;
                                    sizes[0]=512; sizes[1]=63; sizes[2]=16; sizes[3]=cylinders;
                                }
                            }

                            if(yet_detected)
                                WriteOut(StringHelper.sprintf(Msg.get("PROGRAM_IMGMOUNT_AUTODET_VALUES"),new Object[] {new Integer(sizes[0]),new Integer(sizes[1]),new Integer(sizes[2]),new Integer(sizes[3])}));
                            else {
                                WriteOut(Msg.get("PROGRAM_IMGMOUNT_INVALID_GEOMETRY"));
                                return;
                            }
                        } catch (Exception e) {
                            WriteOut(Msg.get("PROGRAM_IMGMOUNT_INVALID_IMAGE"));
                            return;
                        }
                    }

                    newdrive=new Drive_fat(temp_line,sizes[0],sizes[1],sizes[2],sizes[3],0);
                    if(!((Drive_fat)newdrive).created_successfully) {
                    	newdrive = null;
                    }
                } else if (fstype.equals("iso")) {
                } else if (fstype.equals("zip")) {
                	newdrive = new Drive_zip(temp_line);
                } else {
                    FileIO newDisk = null;
                    try {
                        newDisk = FileIOFactory.open(temp_line, FileIOFactory.MODE_READ|FileIOFactory.MODE_WRITE);
                        imagesize = (newDisk.length() / 1024);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    newImage = new Bios_disk.imageDisk(newDisk, temp_line, imagesize, (imagesize > 2880));
                    if(imagesize>2880) newImage.Set_Geometry(sizes[2],sizes[3],sizes[1],sizes[0]);
                }
            } else {
                WriteOut(Msg.get("PROGRAM_IMGMOUNT_TYPE_UNSUPPORTED"),new Object[]{type});
                return;
            }

            if (fstype.equals("fat")) {
                if (Dos_files.Drives[drive-'A']!=null) {
                    WriteOut(Msg.get("PROGRAM_IMGMOUNT_ALREADY_MOUNTED"));
                    return;
                }
                if (newdrive==null) {WriteOut(Msg.get("PROGRAM_IMGMOUNT_CANT_CREATE"));return;}
                Dos_files.Drives[drive-'A']=newdrive;
                // Set the correct media byte in the table
                Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.mediaid)+(drive-'A')*2,mediaid);
                WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_2"),new Object[]{new Character(drive),temp_line});
                if (((Drive_fat)newdrive).loadedDisk.hardDrive) {
                    if (Bios_disk.imageDiskList[2] == null) {
                        Bios_disk.imageDiskList[2] = ((Drive_fat)newdrive).loadedDisk;
                        Bios_disk.imageDisk imageDisk = Bios_disk.imageDiskList[2];
                        if (ide_index.value >= 0) IDE.IDE_Attach(false, ide_index.value, ide_slave.value, imageDisk.diskimg, (int) imageDisk.cylinders, (int) imageDisk.heads, (int) imageDisk.sectors);
                        Bios_disk.updateDPT();
                        return;
                    }
                    if (Bios_disk.imageDiskList[3] == null) {
                        Bios_disk.imageDiskList[3] = ((Drive_fat)newdrive).loadedDisk;
                        Bios_disk.imageDisk imageDisk = Bios_disk.imageDiskList[3];
                        if (ide_index.value >= 0) IDE.IDE_Attach(false, ide_index.value, ide_slave.value, imageDisk.diskimg, (int) imageDisk.cylinders, (int) imageDisk.heads, (int) imageDisk.sectors);
                        Bios_disk.updateDPT();
                        return;
                    }
                }
                if (!((Drive_fat)newdrive).loadedDisk.hardDrive) {
                    Bios_disk.imageDiskList[0] = ((Drive_fat)newdrive).loadedDisk;
                    Floppy.Attach(0, Bios_disk.imageDiskList[0].diskimg);
                }
            } else if (fstype.equals("iso")) {
                // If instructed, attach to IDE controller as ATAPI CD-ROM device
			    if (ide_index.value >= 0) {
                    try  {
                        IDE.IDE_Attach(true, ide_index.value,ide_slave.value, FileIOFactory.open((String)paths.elementAt(0), FileIOFactory.MODE_READ), 0, 0 , 0);
                        WriteOut("Attached ISO to IDE\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (Dos_files.Drives[drive-'A']!=null) {
                    WriteOut(Msg.get("PROGRAM_IMGMOUNT_ALREADY_MOUNTED"));
                    return;
                }
                DosMSCDEX.MSCDEX_SetCDInterface(0, -1);
                // create new drives for all images
                Vector isoDisks = new Vector();
                int i;
                int ct;
                for (i = 0; i < paths.size(); i++) {
                    IntRef error = new IntRef(-1);
                    Dos_Drive newDrive = new Drive_iso(drive, (String)paths.elementAt(i), mediaid, error);
                    isoDisks.add(newDrive);
                    switch (error.value) {
                        case 0  :	break;
                        case 1  :	WriteOut(Msg.get("MSCDEX_ERROR_MULTIPLE_CDROMS"));	break;
                        case 2  :	WriteOut(Msg.get("MSCDEX_ERROR_NOT_SUPPORTED"));	break;
                        case 3  :	WriteOut(Msg.get("MSCDEX_ERROR_OPEN"));				break;
                        case 4  :	WriteOut(Msg.get("MSCDEX_TOO_MANY_DRIVES"));		break;
                        case 5  :	WriteOut(Msg.get("MSCDEX_LIMITED_SUPPORT"));		break;
                        case 6  :	WriteOut(Msg.get("MSCDEX_INVALID_FILEFORMAT"));		break;
                        default :	WriteOut(Msg.get("MSCDEX_UNKNOWN_ERROR"));			break;
                    }
                    // error: clean up and leave
                    if (error.value != 0) {
//                        for(ct = 0; ct < isoDisks.size(); ct++) {
//                            isoDisks.elementAt(ct).close();
//                        }
                        return;
                    }
                }
                // Update DriveManager
                for(ct = 0; ct < isoDisks.size(); ct++) {
                    DriveManager.AppendDisk(drive - 'A', (Dos_Drive)isoDisks.elementAt(ct));
                }
                DriveManager.InitializeDrive(drive - 'A');

                // Set the correct media byte in the table
                Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.mediaid) + (drive - 'A') * 2, mediaid);

                // Print status message (success)
                WriteOut(Msg.get("MSCDEX_SUCCESS"));
                String tmp = (String)paths.elementAt(0);
                for (i = 1; i < paths.size(); i++) {
                    tmp += "; " + paths.elementAt(i);
                }
                WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_2"), new Object[]{new Character(drive), tmp});

            } else if (fstype.equals("zip")) {
                if (Dos_files.Drives[drive-'A']!=null) {
                    WriteOut(Msg.get("PROGRAM_IMGMOUNT_ALREADY_MOUNTED"));
                    return;
                }
                if (newdrive==null) {WriteOut(Msg.get("PROGRAM_IMGMOUNT_CANT_CREATE"));return;}
                Dos_files.Drives[drive-'A']=newdrive;
                Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.mediaid) + (drive - 'A') * 2, mediaid);
                WriteOut(Msg.get("PROGRAM_MOUNT_STATUS_2"),new Object[]{new Character(drive),temp_line});
            } else if (fstype.equals("none")) {
                //if(Bios_disk.imageDiskList[drive-'0'] != null) delete imageDiskList[drive-'0'];
                Bios_disk.imageDiskList[drive-'0'] = newImage;
                Bios_disk.updateDPT();
                WriteOut(Msg.get("PROGRAM_IMGMOUNT_MOUNT_NUMBER"),new Object[]{new Integer(drive-'0'),temp_line});
                // If instructed, attach to IDE controller as ATA hard disk
			    if (ide_index.value >= 0 && drive>='2') IDE.IDE_Attach(false, ide_index.value,ide_slave.value, newImage.diskimg, (int)newImage.cylinders, (int)newImage.heads, (int)newImage.sectors);
                if (drive==48)
                    Floppy.Attach(0, Bios_disk.imageDiskList[0].diskimg);
                else if (drive==49)
                    Floppy.Attach(1, Bios_disk.imageDiskList[1].diskimg);
            }

            // check if volume label is given. becareful for cdrom
            //if (cmd->FindString("-label",label,true)) newdrive->dirCache.SetLabel(label.c_str());
        }
    }


    static private Program.PROGRAMS_Main IMGMOUNT_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new IMGMOUNT();
        }
    };

    private static class KEYB extends Program {
        public void Run() {
            if ((temp_line=cmd.FindCommand(1))!=null) {
                if ((temp_line=cmd.FindString("?",false))!=null) {
                    WriteOut(Msg.get("PROGRAM_KEYB_SHOWHELP"));
                } else {
                    /* first parameter is layout ID */
                    /*Bitu*/int keyb_error=0;
                    String cp_string;
                    /*Bit32s*/IntRef tried_cp = new IntRef(-1);
                    if ((cp_string=cmd.FindCommand(2))!=null) {
                        /* second parameter is codepage number */
                        try {tried_cp.value=Integer.parseInt(cp_string);} catch (Exception e) {}
                        String cp_file_name;
                        if ((cp_string=cmd.FindCommand(3))!=null) {
                            /* third parameter is codepage file */
                            cp_file_name = cp_string;
                        } else {
                            /* no codepage file specified, use automatic selection */
                            cp_file_name = "auto";
                        }

                        keyb_error=Dos_keyboard_layout.DOS_LoadKeyboardLayout(temp_line, tried_cp.value, cp_file_name);
                    } else {
                        keyb_error=Dos_keyboard_layout.DOS_SwitchKeyboardLayout(temp_line, tried_cp);
                    }
                    switch (keyb_error) {
                        case Dos_keyboard_layout.KEYB_NOERROR:
                            WriteOut(Msg.get("PROGRAM_KEYB_NOERROR"),new Object[]{temp_line,new Integer(Dos.dos.loaded_codepage)});
                            break;
                        case Dos_keyboard_layout.KEYB_FILENOTFOUND:
                            WriteOut(Msg.get("PROGRAM_KEYB_FILENOTFOUND"),new Object[]{temp_line});
                            WriteOut(Msg.get("PROGRAM_KEYB_SHOWHELP"));
                            break;
                        case Dos_keyboard_layout.KEYB_INVALIDFILE:
                            WriteOut(Msg.get("PROGRAM_KEYB_INVALIDFILE"),new Object[]{temp_line});
                            break;
                        case Dos_keyboard_layout.KEYB_LAYOUTNOTFOUND:
                            WriteOut(Msg.get("PROGRAM_KEYB_LAYOUTNOTFOUND"),new Object[]{temp_line,new Integer(tried_cp.value)});
                            break;
                        case Dos_keyboard_layout.KEYB_INVALIDCPFILE:
                            WriteOut(Msg.get("PROGRAM_KEYB_INVCPFILE"),new Object[]{temp_line});
                            WriteOut(Msg.get("PROGRAM_KEYB_SHOWHELP"));
                            break;
                        default:
                            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC, LogSeverities.LOG_ERROR,"KEYB:Invalid returncode "+Integer.toString(keyb_error,16));
                            break;
                    }
                }
            } else {
                /* no parameter in the command line, just output codepage info and possibly loaded layout ID */
                String layout_name = Dos_keyboard_layout.DOS_GetLoadedLayout();
                if (layout_name==null) {
                    WriteOut(Msg.get("PROGRAM_KEYB_INFO"),new Object[]{new Integer(Dos.dos.loaded_codepage)});
                } else {
                    WriteOut(Msg.get("PROGRAM_KEYB_INFO_LAYOUT"),new Object[]{new Integer(Dos.dos.loaded_codepage),layout_name});
                }
            }
        }
    }

    static private Program.PROGRAMS_Main KEYB_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new KEYB();
        }
    };

    public static void DOS_SetupPrograms() {
        /*Add Messages */

        Msg.add("PROGRAM_MOUNT_CDROMS_FOUND","CDROMs found: %d\n");
        Msg.add("PROGRAM_MOUNT_STATUS_FORMAT", "%-5s  %-58s %-12s\n");
        Msg.add("PROGRAM_MOUNT_STATUS_2","Drive %c is mounted as %s\n");
        Msg.add("PROGRAM_MOUNT_STATUS_1","The currently mounted drives are:\n");
        Msg.add("PROGRAM_MOUNT_ERROR_1","Directory %s doesn't exist.\n");
        Msg.add("PROGRAM_MOUNT_ERROR_2","%s isn't a directory\n");
        Msg.add("PROGRAM_MOUNT_ILL_TYPE","Illegal type %s\n");
        Msg.add("PROGRAM_MOUNT_ALREADY_MOUNTED","Drive %c already mounted with %s\n");
        Msg.add("PROGRAM_MOUNT_USAGE",
            "Usage \033[34;1mMOUNT Drive-Letter Local-Directory\033[0m\n" +
            "For example: MOUNT c %s\n" +
            "This makes the directory %s act as the C: drive inside DOSBox.\n" +
            "The directory has to exist.\n");
        Msg.add("PROGRAM_MOUNT_UMOUNT_NOT_MOUNTED","Drive %c isn't mounted.\n");
        Msg.add("PROGRAM_MOUNT_UMOUNT_SUCCESS","Drive %c has successfully been removed.\n");
        Msg.add("PROGRAM_MOUNT_UMOUNT_NO_VIRTUAL","Virtual Drives can not be unMOUNTed.\n");
        Msg.add("PROGRAM_MOUNT_WARNING_WIN","\033[31;1mMounting c:\\ is NOT recommended. Please mount a (sub)directory next time.\033[0m\n");
        Msg.add("PROGRAM_MOUNT_WARNING_OTHER","\033[31;1mMounting / is NOT recommended. Please mount a (sub)directory next time.\033[0m\n");

        Msg.add("PROGRAM_MEM_CONVEN","%10d Kb free conventional memory\n");
        Msg.add("PROGRAM_MEM_EXTEND","%10d Kb free extended memory\n");
        Msg.add("PROGRAM_MEM_EXPAND","%10d Kb free expanded memory\n");
        Msg.add("PROGRAM_MEM_UPPER","%10d Kb free upper memory in %d blocks (largest UMB %d Kb)\n");

        Msg.add("PROGRAM_LOADFIX_ALLOC","%d kb allocated.\n");
        Msg.add("PROGRAM_LOADFIX_DEALLOC","%d kb freed.\n");
        Msg.add("PROGRAM_LOADFIX_DEALLOCALL","Used memory freed.\n");
        Msg.add("PROGRAM_LOADFIX_ERROR","Memory allocation error.\n");

        Msg.add("MSCDEX_SUCCESS","MSCDEX installed.\n");
        Msg.add("MSCDEX_ERROR_MULTIPLE_CDROMS","MSCDEX: Failure: Drive-letters of multiple CDRom-drives have to be continuous.\n");
        Msg.add("MSCDEX_ERROR_NOT_SUPPORTED","MSCDEX: Failure: Not yet supported.\n");
        Msg.add("MSCDEX_ERROR_OPEN","MSCDEX: Failure: Invalid file or unable to open.\n");
        Msg.add("MSCDEX_TOO_MANY_DRIVES","MSCDEX: Failure: Too many CDRom-drives (max: 5). MSCDEX Installation failed.\n");
        Msg.add("MSCDEX_LIMITED_SUPPORT","MSCDEX: Mounted subdirectory: limited support.\n");
        Msg.add("MSCDEX_INVALID_FILEFORMAT","MSCDEX: Failure: File is either no iso/cue image or contains errors.\n");
        Msg.add("MSCDEX_UNKNOWN_ERROR","MSCDEX: Failure: Unknown error.\n");

        Msg.add("PROGRAM_RESCAN_SUCCESS","Drive cache cleared.\n");

        Msg.add("PROGRAM_INTRO",
            "\033[2J\033[32;1mWelcome to DOSBox\033[0m, an x86 emulator with sound and graphics.\n" +
            "DOSBox creates a shell for you which looks like old plain DOS.\n" +
            "\n" +
            "For information about basic mount type \033[34;1mintro mount\033[0m\n" +
            "For information about CD-ROM support type \033[34;1mintro cdrom\033[0m\n" +
            "For information about special keys type \033[34;1mintro special\033[0m\n" +
            "For more information about DOSBox, go to \033[34;1mhttp://www.dosbox.com/wiki\033[0m\n" +
            "\n" +
            "\033[31;1mDOSBox will stop/exit without a warning if an error occured!\033[0m\n" +
            "\n" +
            "\n"
            );
        Msg.add("PROGRAM_INTRO_MOUNT_START",
            "\033[32;1mHere are some commands to get you started:\033[0m\n" +
            "Before you can use the files located on your own filesystem,\n" +
            "You have to mount the directory containing the files.\n" +
            "\n"
            );
        Msg.add("PROGRAM_INTRO_MOUNT_WINDOWS",
            "\033[44;1m\u00C9\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BB\n" +
            "\u00BA \033[32mmount c c:\\dosprogs\\\033[37m will create a C drive with c:\\dosprogs as contents.\u00BA\n" +
            "\u00BA                                                                         \u00BA\n" +
            "\u00BA \033[32mc:\\dosprogs\\\033[37m is an example. Replace it with your own games directory.  \033[37m \u00BA\n" +
            "\u00C8\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BC\033[0m\n"
            );
        Msg.add("PROGRAM_INTRO_MOUNT_OTHER",
            "\033[44;1m\u00C9\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BB\n" +
            "\u00BA \033[32mmount c ~/dosprogs\033[37m will create a C drive with ~/dosprogs as contents.\u00BA\n" +
            "\u00BA                                                                      \u00BA\n" +
            "\u00BA \033[32m~/dosprogs\033[37m is an example. Replace it with your own games directory.\033[37m  \u00BA\n" +
            "\u00C8\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
            "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BC\033[0m\n"
            );
        Msg.add("PROGRAM_INTRO_MOUNT_END",
            "When the mount has successfully completed you can type \033[34;1mc:\033[0m to go to your freshly\n" +
            "mounted C-drive. Typing \033[34;1mdir\033[0m there will show its contents." +
            " \033[34;1mcd\033[0m will allow you to\n" +
            "enter a directory (recognised by the \033[33;1m[]\033[0m in a directory listing).\n" +
            "You can run programs/files which end with \033[31m.exe .bat\033[0m and \033[31m.com\033[0m.\n"
            );
        Msg.add("PROGRAM_INTRO_CDROM",
            "\033[2J\033[32;1mHow to mount a Real/Virtual CD-ROM Drive in DOSBox:\033[0m\n" +
            "DOSBox provides CD-ROM emulation on several levels.\n" +
            "\n" +
            "The \033[33mbasic\033[0m level works on all CD-ROM drives and normal directories.\n" +
            "It installs MSCDEX and marks the files read-only.\n" +
            "Usually this is enough for most games:\n" +
            "\033[34;1mmount d \033[0;31mD:\\\033[34;1m -t cdrom\033[0m   or   \033[34;1mmount d C:\\example -t cdrom\033[0m\n" +
            "If it doesn't work you might have to tell DOSBox the label of the CD-ROM:\n" +
            "\033[34;1mmount d C:\\example -t cdrom -label CDLABEL\033[0m\n" +
            "\n" +
            "The \033[33mnext\033[0m level adds some low-level support.\n" +
            "Therefore only works on CD-ROM drives:\n" +
            "\033[34;1mmount d \033[0;31mD:\\\033[34;1m -t cdrom -usecd \033[33m0\033[0m\n" +
            "\n" +
            "The \033[33mlast\033[0m level of support depends on your Operating System:\n" +
            "For \033[1mWindows 2000\033[0m, \033[1mWindows XP\033[0m and \033[1mLinux\033[0m:\n" +
            "\033[34;1mmount d \033[0;31mD:\\\033[34;1m -t cdrom -usecd \033[33m0 \033[34m-ioctl\033[0m\n" +
            "For \033[1mWindows 9x\033[0m with a ASPI layer installed:\n" +
            "\033[34;1mmount d \033[0;31mD:\\\033[34;1m -t cdrom -usecd \033[33m0 \033[34m-aspi\033[0m\n" +
            "\n" +
            "Replace \033[0;31mD:\\\033[0m with the location of your CD-ROM.\n" +
            "Replace the \033[33;1m0\033[0m in \033[34;1m-usecd \033[33m0\033[0m with the number reported for your CD-ROM if you type:\n"+
            "\033[34;1mmount -cd\033[0m\n"
            );
        Msg.add("PROGRAM_INTRO_SPECIAL",
            "\033[2J\033[32;1mSpecial keys:\033[0m\n" +
            "These are the default keybindings.\n" +
            "They can be changed in the \033[33mkeymapper\033[0m.\n" +
            "\n" +
            "\033[33;1mALT-ENTER\033[0m   : Go full screen and back.\n" +
            "\033[33;1mALT-PAUSE\033[0m   : Pause DOSBox.\n" +
            "\033[33;1mCTRL-F1\033[0m     : Start the \033[33mkeymapper\033[0m.\n" +
            "\033[33;1mCTRL-F4\033[0m     : Update directory cache for all drives! Swap mounted disk-image.\n" +
            "\033[33;1mCTRL-ALT-F5\033[0m : Start/Stop creating a movie of the screen.\n" +
            "\033[33;1mCTRL-F5\033[0m     : Save a screenshot.\n" +
            "\033[33;1mCTRL-F6\033[0m     : Start/Stop recording sound output to a wave file.\n" +
            "\033[33;1mCTRL-ALT-F7\033[0m : Start/Stop recording of OPL commands.\n" +
            "\033[33;1mCTRL-ALT-F8\033[0m : Start/Stop the recording of raw MIDI commands.\n" +
            "\033[33;1mCTRL-F7\033[0m     : Decrease frameskip.\n" +
            "\033[33;1mCTRL-F8\033[0m     : Increase frameskip.\n" +
            "\033[33;1mCTRL-F9\033[0m     : Kill DOSBox.\n" +
            "\033[33;1mCTRL-F10\033[0m    : Capture/Release the mouse.\n" +
            "\033[33;1mCTRL-F11\033[0m    : Slow down emulation (Decrease DOSBox Cycles).\n" +
            "\033[33;1mCTRL-F12\033[0m    : Speed up emulation (Increase DOSBox Cycles).\n" +
            "\033[33;1mALT-F12\033[0m     : Unlock speed (turbo button/fast forward).\n"
            );
        Msg.add("PROGRAM_BOOT_NOT_EXIST","Bootdisk file does not exist.  Failing.\n");
        Msg.add("PROGRAM_BOOT_NOT_OPEN","Cannot open bootdisk file.  Failing.\n");
        Msg.add("PROGRAM_BOOT_WRITE_PROTECTED","Image file is read-only! Might create problems.\n");
        Msg.add("PROGRAM_BOOT_PRINT_ERROR","This command boots DOSBox from either a floppy or hard disk image.\n\n" +
            "For this command, one can specify a succession of floppy disks swappable\n" +
            "by pressing Ctrl-F4, and -l specifies the mounted drive to boot from.  If\n" +
            "no drive letter is specified, this defaults to booting from the A drive.\n" +
            "The only bootable drive letters are A, C, and D.  For booting from a hard\n" +
            "drive (C or D), the image should have already been mounted using the\n" +
            "\033[34;1mIMGMOUNT\033[0m command.\n\n" +
            "The syntax of this command is:\n\n" +
            "\033[34;1mBOOT [diskimg1.img diskimg2.img] [-l driveletter]\033[0m\n"
            );
        Msg.add("PROGRAM_BOOT_UNABLE","Unable to boot off of drive %c");
        Msg.add("PROGRAM_BOOT_IMAGE_OPEN","Opening image file: %s\n");
        Msg.add("PROGRAM_BOOT_IMAGE_NOT_OPEN","Cannot open %s");
        Msg.add("PROGRAM_BOOT_BOOT","Booting from drive %c...\n");
        Msg.add("PROGRAM_BOOT_CART_WO_PCJR","PCjr cartridge found, but machine is not PCjr");
        Msg.add("PROGRAM_BOOT_CART_LIST_CMDS","Available PCjr cartridge commandos:%s");
        Msg.add("PROGRAM_BOOT_CART_NO_CMDS","No PCjr cartridge commandos found");

        Msg.add("PROGRAM_IMGMOUNT_SPECIFY_DRIVE","Must specify drive letter to mount image at.\n");
        Msg.add("PROGRAM_IMGMOUNT_SPECIFY2","Must specify drive number (0 or 3) to mount image at (0,1=fda,fdb;2,3=hda,hdb).\n");
        Msg.add("PROGRAM_IMGMOUNT_SPECIFY_GEOMETRY",
            "For \033[33mCD-ROM\033[0m images:   \033[34;1mIMGMOUNT drive-letter location-of-image -t iso\033[0m\n" +
            "\n" +
            "For \033[33mhardrive\033[0m images: Must specify drive geometry for hard drives:\n" +
            "bytes_per_sector, sectors_per_cylinder, heads_per_cylinder, cylinder_count.\n" +
            "\033[34;1mIMGMOUNT drive-letter location-of-image -size bps,spc,hpc,cyl\033[0m\n");
        Msg.add("PROGRAM_IMGMOUNT_INVALID_IMAGE","Could not load image file.\n" +
            "Check that the path is correct and the image is accessible.\n");
        Msg.add("PROGRAM_IMGMOUNT_INVALID_GEOMETRY","Could not extract drive geometry from image.\n" +
            "Use parameter -size bps,spc,hpc,cyl to specify the geometry.\n");
        Msg.add("PROGRAM_IMGMOUNT_AUTODET_VALUES","Image geometry auto detection: -size %d,%d,%d,%d\n");
        Msg.add("PROGRAM_IMGMOUNT_TYPE_UNSUPPORTED","Type \"%s\" is unsupported. Specify \"hdd\" or \"floppy\" or\"iso\".\n");
        Msg.add("PROGRAM_IMGMOUNT_FORMAT_UNSUPPORTED","Format \"%s\" is unsupported. Specify \"fat\" or \"iso\" or \"none\".\n");
        Msg.add("PROGRAM_IMGMOUNT_SPECIFY_FILE","Must specify file-image to mount.\n");
        Msg.add("PROGRAM_IMGMOUNT_FILE_NOT_FOUND","Image file not found.\n");
        Msg.add("PROGRAM_IMGMOUNT_MOUNT","To mount directories, use the \033[34;1mMOUNT\033[0m command, not the \033[34;1mIMGMOUNT\033[0m command.\n");
        Msg.add("PROGRAM_IMGMOUNT_ALREADY_MOUNTED","Drive already mounted at that letter.\n");
        Msg.add("PROGRAM_IMGMOUNT_CANT_CREATE","Can't create drive from file.\n");
        Msg.add("PROGRAM_IMGMOUNT_MOUNT_NUMBER","Drive number %d mounted as %s\n");
        Msg.add("PROGRAM_IMGMOUNT_NON_LOCAL_DRIVE", "The image must be on a host or local drive.\n");
        Msg.add("PROGRAM_IMGMOUNT_MULTIPLE_NON_CUEISO_FILES", "Using multiple files is only supported for cue/iso images.\n");

        Msg.add("PROGRAM_KEYB_INFO","Codepage %i has been loaded\n");
        Msg.add("PROGRAM_KEYB_INFO_LAYOUT","Codepage %i has been loaded for layout %s\n");
        Msg.add("PROGRAM_KEYB_SHOWHELP",
            "\033[32;1mKEYB\033[0m [keyboard layout ID[ codepage number[ codepage file]]]\n\n" +
            "Some examples:\n" +
            "  \033[32;1mKEYB\033[0m: Display currently loaded codepage.\n" +
            "  \033[32;1mKEYB\033[0m sp: Load the spanish (SP) layout, use an appropriate codepage.\n" +
            "  \033[32;1mKEYB\033[0m sp 850: Load the spanish (SP) layout, use codepage 850.\n" +
            "  \033[32;1mKEYB\033[0m sp 850 mycp.cpi: Same as above, but use file mycp.cpi.\n");
        Msg.add("PROGRAM_KEYB_NOERROR","Keyboard layout %s loaded for codepage %i\n");
        Msg.add("PROGRAM_KEYB_FILENOTFOUND","Keyboard file %s not found\n\n");
        Msg.add("PROGRAM_KEYB_INVALIDFILE","Keyboard file %s invalid\n");
        Msg.add("PROGRAM_KEYB_LAYOUTNOTFOUND","No layout in %s for codepage %i\n");
        Msg.add("PROGRAM_KEYB_INVCPFILE","None or invalid codepage file for layout %s\n\n");

        /*regular setup*/
        Program.PROGRAMS_MakeFile("MOUNT.COM",MOUNT_ProgramStart);
        Program.PROGRAMS_MakeFile("MEM.COM",MEM_ProgramStart);
        Program.PROGRAMS_MakeFile("LOADFIX.COM",LOADFIX_ProgramStart);
        Program.PROGRAMS_MakeFile("RESCAN.COM",RESCAN_ProgramStart);
        Program.PROGRAMS_MakeFile("INTRO.COM",INTRO_ProgramStart);
        Program.PROGRAMS_MakeFile("BOOT.COM",BOOT_ProgramStart);
//    #if C_DEBUG
//        PROGRAMS_MakeFile("LDGFXROM.COM", LDGFXROM_ProgramStart);
//    #endif
        Program.PROGRAMS_MakeFile("IMGMOUNT.COM", IMGMOUNT_ProgramStart);
        Program.PROGRAMS_MakeFile("KEYB.COM", KEYB_ProgramStart);
        Program.PROGRAMS_MakeFile("REBOOT.COM", REBOOT_ProgramStart);
    }

}
