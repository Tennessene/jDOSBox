package jdos.shell;

import jdos.Dosbox;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.*;
import jdos.dos.drives.Drive_virtual;
import jdos.hardware.Memory;
import jdos.misc.Msg;
import jdos.misc.Program;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_line;
import jdos.util.IntRef;
import jdos.util.StringHelper;

import java.io.File;

public class Shell {
    static final public int CMD_MAXLINE = 4096;
    static final public int CMD_MAXCMDS = 20;
    static final public int CMD_OLDSIZE = 4096;       

    private static /*Bitu*/int call_shellstop;
/* Larger scope so shell_del autoexec can use it to
 * remove things from the environment */
    public static Program first_shell; 

    static private Callback.Handler shellstop_handler = new Callback.Handler() {
        public String getName() {
            return "Shell.shellstop_handler";
        }
        public /*Bitu*/int call() {
            return Callback.CBRET_STOP;
        }
    };

    static private Program.PROGRAMS_Main SHELL_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new Dos_shell();
        }
    };

    private static class AUTOEXEC extends Module_base {
        private AutoexecObject[] autoexec = new AutoexecObject[17];
        private AutoexecObject autoexec_echo = new AutoexecObject();
        public AUTOEXEC(Section configuration) {
            super(configuration);
            for (int i=0;i<autoexec.length;i++) {
                autoexec[i] = new AutoexecObject();
            }
            /* Register a virtual AUOEXEC.BAT file */
            Section_line section=(Section_line)configuration;

            /* Check -securemode switch to disable mount/imgmount/boot after running autoexec.bat */
            boolean secure = Dosbox.control.cmdline.FindExist("-securemode",true);

            /* add stuff from the configfile unless -noautexec or -securemode is specified. */
            String extra = section.data;
            if (extra!=null && !secure && !Dosbox.control.cmdline.FindExist("-noautoexec",true)) {
                /* detect if "echo off" is the first line */
                boolean echo_off  = extra.startsWith("echo off");
                if (!echo_off) echo_off = extra.startsWith("@echo off",9);

                /* if "echo off" add it to the front of autoexec.bat */
                if(echo_off) autoexec_echo.InstallBefore("@echo off");

                /* Install the stuff from the configfile */
                autoexec[0].Install(section.data);
            }

            /* Check to see for extra command line options to be added (before the command specified on commandline) */
            /* Maximum of extra commands: 10 */
            /*Bitu*/int i = 1;
            String line;
            while ((line=Dosbox.control.cmdline.FindString("-c",true))!=null && (i <= 11)) {
                //replace single with double quotes so that mount commands can contain spaces
                StringHelper.replace(line, "\'", "\"");
                autoexec[i++].Install(line);
            }

            /* Check for the -exit switch which causes dosbox to when the command on the commandline has finished */
            boolean addexit = Dosbox.control.cmdline.FindExist("-exit",true);

            /* Check for first command being a directory or file */
            /* Combining -securemode and no parameter leaves you with a lovely Z:\. */
            if ((line=Dosbox.control.cmdline.FindCommand(1))==null) {
                if ( secure ) autoexec[12].Install("z:\\config.com -securemode");
            } else {
                String buffer = line;
                if (!new File(buffer).exists()){
                    try {
                        buffer = new File( "." ).getCanonicalPath()+File.separator+line;
                        if (!new File(buffer).exists()) buffer = null;
                    } catch (Exception e) {
                        buffer = null;
                    }
                }
                if (buffer != null) {
                    if (new File(buffer).isDirectory()) {
                        autoexec[12].Install("MOUNT C \"" + buffer + "\"");
                        autoexec[13].Install("C:");
                        if(secure) autoexec[14].Install("z:\\config.com -securemode");
                    } else {
                        int name_pos = buffer.lastIndexOf(File.separator);
                        if (name_pos<0) { //Only a filename
                            line = buffer;
                            try {
                                buffer = new File( "." ).getCanonicalPath()+File.separator+line;
                                if (!new File(buffer).exists()) buffer = null;
                            } catch (Exception e) {
                                buffer = null;
                            }
                            if (buffer != null) {
                                name_pos = buffer.lastIndexOf(File.separator);
                                if (name_pos<0) {
                                    buffer = null;
                                }
                            }
                        }
                        if (buffer != null && new File(buffer).exists()) {
                            autoexec[12].Install("MOUNT C \"" + buffer + "\"");
                            autoexec[13].Install("C:");
                            /* Save the non-modified filename (so boot and imgmount can use it (long filenames, case sensivitive) */
                            String name = buffer.substring(name_pos+1);
                            String orig = name;
                            name = name.toUpperCase();
                            if(name.indexOf(".BAT")>=0) {
                                if(secure) autoexec[14].Install("z:\\config.com -securemode");
                                /* BATch files are called else exit will not work */
                                autoexec[15].Install("CALL " + name);
                                if(addexit) autoexec[16].Install("exit");
                            } else if(name.indexOf(".IMG")>=0 || name.indexOf(".IMA")>=0) {
                                //No secure mode here as boot is destructive and enabling securemode disables boot
                                /* Boot image files */
                                autoexec[15].Install("BOOT " + orig);
                            } else if(name.indexOf(".ISO") != 0 || name.indexOf(".CUE")>=0) {
                                /* imgmount CD image files */
                                /* securemode gets a different number from the previous branches! */
                                autoexec[14].Install("IMGMOUNT D \"" + orig + "\" -t iso");
                                //autoexec[16].Install("D:");
                                if(secure) autoexec[15].Install("z:\\config.com -securemode");
                                /* Makes no sense to exit here */

                            } else {
                                if(secure) autoexec[14].Install("z:\\config.com -securemode");
                                autoexec[15].Install(name);
                                if(addexit) autoexec[16].Install("exit");
                            }
                        }
                    }
                }
            }
            byte[] b = AutoexecObject.autoexec_data.toString().getBytes();
            Drive_virtual.VFILE_Remove("AUTOEXEC.BAT");
            Drive_virtual.VFILE_Register("AUTOEXEC.BAT",b,b.length);
        }
    }

    static AUTOEXEC test;

    public static Section.SectionFunction AUTOEXEC_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
            AutoexecObject.Shutdown();
        }
    };

    public static Section.SectionFunction AUTOEXEC_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new AUTOEXEC(section);
            section.AddDestroyFunction(AUTOEXEC_Destroy,false);
        }
    };

    static private final String path_string="PATH=Z:\\";
    static private final String comspec_string="COMSPEC=Z:\\COMMAND.COM";
    static private final String full_name="Z:\\COMMAND.COM";
    static private final String init_line="/INIT AUTOEXEC.BAT";

    public static Config.StartFunction SHELL_Init = new Config.StartFunction() {
        public void call() {
            Msg.add("SHELL_ILLEGAL_PATH","Illegal Path.\n");
            Msg.add("SHELL_CMD_HELP","If you want a list of all supported commands type \033[33;1mhelp /all\033[0m .\nA short list of the most often used commands:\n");
            Msg.add("SHELL_CMD_ECHO_ON","ECHO is on.\n");
            Msg.add("SHELL_CMD_ECHO_OFF","ECHO is off.\n");
            Msg.add("SHELL_ILLEGAL_SWITCH","Illegal switch: %s.\n");
            Msg.add("SHELL_MISSING_PARAMETER","Required parameter missing.\n");
            Msg.add("SHELL_CMD_CHDIR_ERROR","Unable to change to: %s.\n");
            Msg.add("SHELL_CMD_CHDIR_HINT","To change to different drive type \033[31m%c:\033[0m\n");
            Msg.add("SHELL_CMD_CHDIR_HINT_2","directoryname is longer than 8 characters and/or contains spaces.\nTry \033[31mcd %s\033[0m\n");
            Msg.add("SHELL_CMD_CHDIR_HINT_3","You are still on drive Z:, change to a mounted drive with \033[31mC:\033[0m.\n");
            Msg.add("SHELL_CMD_DATE_HELP","Displays or changes the internal date.\n");
            Msg.add("SHELL_CMD_DATE_ERROR","The specified date is not correct.\n");
            Msg.add("SHELL_CMD_DATE_DAYS","3SunMonTueWedThuFriSat"); // "2SoMoDiMiDoFrSa"
            Msg.add("SHELL_CMD_DATE_NOW","Current date: ");
            Msg.add("SHELL_CMD_DATE_SETHLP","Type 'date MM-DD-YYYY' to change.\n");
            Msg.add("SHELL_CMD_DATE_FORMAT","M/D/Y");
            Msg.add("SHELL_CMD_DATE_HELP_LONG","DATE [[/T] [/H] [/S] | MM-DD-YYYY]\n" +
                                            "  MM-DD-YYYY: new date to set\n" +
                                            "  /S:         Permanently use host time and date as DOS time\n" +
                                            "  /F:         Switch back to DOSBox internal time (opposite of /S)\n" +
                                            "  /T:         Only display date\n" +
                                            "  /H:         Synchronize with host\n");
            Msg.add("SHELL_CMD_TIME_HELP","Displays the internal time.\n");
            Msg.add("SHELL_CMD_TIME_NOW","Current time: ");
            Msg.add("SHELL_CMD_TIME_HELP_LONG","TIME [/T] [/H]\n" +
                                            "  /T:         Display simple time\n" +
                                            "  /H:         Synchronize with host\n");

            Msg.add("SHELL_CMD_MKDIR_ERROR","Unable to make: %s.\n");
            Msg.add("SHELL_CMD_RMDIR_ERROR","Unable to remove: %s.\n");
            Msg.add("SHELL_CMD_DEL_ERROR","Unable to delete: %s.\n");
            Msg.add("SHELL_SYNTAXERROR","The syntax of the command is incorrect.\n");
            Msg.add("SHELL_CMD_SET_NOT_SET","Environment variable %s not defined.\n");
            Msg.add("SHELL_CMD_SET_OUT_OF_SPACE","Not enough environment space left.\n");
            Msg.add("SHELL_CMD_IF_EXIST_MISSING_FILENAME","IF EXIST: Missing filename.\n");
            Msg.add("SHELL_CMD_IF_ERRORLEVEL_MISSING_NUMBER","IF ERRORLEVEL: Missing number.\n");
            Msg.add("SHELL_CMD_IF_ERRORLEVEL_INVALID_NUMBER","IF ERRORLEVEL: Invalid number.\n");
            Msg.add("SHELL_CMD_GOTO_MISSING_LABEL","No label supplied to GOTO command.\n");
            Msg.add("SHELL_CMD_GOTO_LABEL_NOT_FOUND","GOTO: Label %s not found.\n");
            Msg.add("SHELL_CMD_FILE_NOT_FOUND","File %s not found.\n");
            Msg.add("SHELL_CMD_FILE_EXISTS","File %s already exists.\n");
            Msg.add("SHELL_CMD_DIR_INTRO","Directory of %s.\n");
            Msg.add("SHELL_CMD_DIR_BYTES_USED","%5d File(s) %17s Bytes.\n");
            Msg.add("SHELL_CMD_DIR_BYTES_FREE","%5d Dir(s)  %17s Bytes free.\n");
            Msg.add("SHELL_EXECUTE_DRIVE_NOT_FOUND","Drive %c does not exist!\nYou must \033[31mmount\033[0m it first. Type \033[1;33mintro\033[0m or \033[1;33mintro mount\033[0m for more information.\n");
            Msg.add("SHELL_EXECUTE_ILLEGAL_COMMAND","Illegal command: %s.\n");
            Msg.add("SHELL_CMD_PAUSE","Press any key to continue.\n");
            Msg.add("SHELL_CMD_PAUSE_HELP","Waits for 1 keystroke to continue.\n");
            Msg.add("SHELL_CMD_COPY_FAILURE","Copy failure : %s.\n");
            Msg.add("SHELL_CMD_COPY_SUCCESS","   %d File(s) copied.\n");
            Msg.add("SHELL_CMD_SUBST_NO_REMOVE","Unable to remove, drive not in use.\n");
            Msg.add("SHELL_CMD_SUBST_FAILURE","SUBST failed. You either made an error in your commandline or the target drive is already used.\nIt's only possible to use SUBST on Local drives");

            Msg.add("SHELL_STARTUP_BEGIN",
                "\033[44;1m\u00C9\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
                "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
                "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BB\n" +
                "\u00BA \033[32mWelcome to DOSBox v%-12s\033[37m                                    \u00BA\n" +
                "\u00BA                                                                    \u00BA\n" +
        //		"\u00BA DOSBox runs real and protected mode games.                         \u00BA\n" +
                "\u00BA For a short introduction for new users type: \033[33mINTRO\033[37m                 \u00BA\n" +
                "\u00BA For supported shell commands type: \033[33mHELP\033[37m                            \u00BA\n" +
                "\u00BA                                                                    \u00BA\n" +
                "\u00BA To adjust the emulated CPU speed, use \033[31mctrl-F11\033[37m and \033[31mctrl-F12\033[37m.       \u00BA\n" +
                "\u00BA To activate the keymapper \033[31mctrl-F1\033[37m.                                 \u00BA\n" +
                "\u00BA For more information read the \033[36mREADME\033[37m file in the DOSBox directory. \u00BA\n" +
                "\u00BA                                                                    \u00BA\n" 
            );
            Msg.add("SHELL_STARTUP_CGA","\u00BA DOSBox supports Composite CGA mode.                                \u00BA\n" +
                    "\u00BA Use \033[31m(alt-)F11\033[37m to change the colours when in this mode.             \u00BA\n" +
                    "\u00BA                                                                    \u00BA\n"
            );
            Msg.add("SHELL_STARTUP_HERC","\u00BA Use \033[31mF11\033[37m to cycle through white, amber, and green monochrome color. \u00BA\n" +
                    "\u00BA                                                                    \u00BA\n"
            );
            Msg.add("SHELL_STARTUP_DEBUG",
                    "\u00BA Press \033[31malt-Pause\033[37m to enter the debugger or start the exe with \033[33mDEBUG\033[37m. \u00BA\n" +
                    "\u00BA                                                                    \u00BA\n"
            );
            Msg.add("SHELL_STARTUP_END",
                    "\u00BA \033[32mHAVE FUN!\033[37m                                                          \u00BA\n" +
                    "\u00BA \033[32mThe DOSBox Team \033[33mhttp://www.dosbox.com\033[37m                              \u00BA\n" +
                    "\u00BA \033[32mPorted to Java by James Bryant \033[33mhttp://jdosbox.sf.net\033[37m               \u00BA\n" +
                    "\u00C8\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
                    "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD" +
                    "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BC\033[0m\n"
                    //"\n" //Breaks the startup message if you type a mount and a drive change.
            );
            Msg.add("SHELL_CMD_CHDIR_HELP","Displays/changes the current directory.\n");
            Msg.add("SHELL_CMD_CHDIR_HELP_LONG","CHDIR [drive:][path]\n" +
                    "CHDIR [..]\n" +
                    "CD [drive:][path]\n" +
                    "CD [..]\n\n" +
                    "  ..   Specifies that you want to change to the parent directory.\n\n" +
                    "Type CD drive: to display the current directory in the specified drive.\n" +
                    "Type CD without parameters to display the current drive and directory.\n");
            Msg.add("SHELL_CMD_CLS_HELP","Clear screen.\n");
            Msg.add("SHELL_CMD_DIR_HELP","Directory View.\n");
            Msg.add("SHELL_CMD_ECHO_HELP","Display messages and enable/disable command echoing.\n");
            Msg.add("SHELL_CMD_EXIT_HELP","Exit from the shell.\n");
            Msg.add("SHELL_CMD_HELP_HELP","Show help.\n");
            Msg.add("SHELL_CMD_MKDIR_HELP","Make Directory.\n");
            Msg.add("SHELL_CMD_MKDIR_HELP_LONG","MKDIR [drive:][path]\n" +
                    "MD [drive:][path]\n");
            Msg.add("SHELL_CMD_RMDIR_HELP","Remove Directory.\n");
            Msg.add("SHELL_CMD_RMDIR_HELP_LONG","RMDIR [drive:][path]\n" +
                    "RD [drive:][path]\n");
            Msg.add("SHELL_CMD_SET_HELP","Change environment variables.\n");
            Msg.add("SHELL_CMD_IF_HELP","Performs conditional processing in batch programs.\n");
            Msg.add("SHELL_CMD_GOTO_HELP","Jump to a labeled line in a batch script.\n");
            Msg.add("SHELL_CMD_SHIFT_HELP","Leftshift commandline parameters in a batch script.\n");
            Msg.add("SHELL_CMD_TYPE_HELP","Display the contents of a text-file.\n");
            Msg.add("SHELL_CMD_TYPE_HELP_LONG","TYPE [drive:][path][filename]\n");
            Msg.add("SHELL_CMD_REM_HELP","Add comments in a batch file.\n");
            Msg.add("SHELL_CMD_REM_HELP_LONG","REM [comment]\n");
            Msg.add("SHELL_CMD_NO_WILD","This is a simple version of the command, no wildcards allowed!\n");
            Msg.add("SHELL_CMD_RENAME_HELP","Renames one or more files.\n");
            Msg.add("SHELL_CMD_RENAME_HELP_LONG","RENAME [drive:][path]filename1 filename2.\n" +
                    "REN [drive:][path]filename1 filename2.\n\n" +
                    "Note that you can not specify a new drive or path for your destination file.\n");
            Msg.add("SHELL_CMD_DELETE_HELP","Removes one or more files.\n");
            Msg.add("SHELL_CMD_COPY_HELP","Copy files.\n");
            Msg.add("SHELL_CMD_CALL_HELP","Start a batch file from within another batch file.\n");
            Msg.add("SHELL_CMD_SUBST_HELP","Assign an internal directory to a drive.\n");
            Msg.add("SHELL_CMD_LOADHIGH_HELP","Loads a program into upper memory (requires xms=true,umb=true).\n");
            Msg.add("SHELL_CMD_CHOICE_HELP","Waits for a keypress and sets ERRORLEVEL.\n");
            Msg.add("SHELL_CMD_CHOICE_HELP_LONG","CHOICE [/C:choices] [/N] [/S] text\n" +
                    "  /C[:]choices  -  Specifies allowable keys.  Default is: yn.\n" +
                    "  /N  -  Do not display the choices at end of prompt.\n" +
                    "  /S  -  Enables case-sensitive choices to be selected.\n"+
                    "  text  -  The text to display as a prompt.\n");
            Msg.add("SHELL_CMD_ATTRIB_HELP","Does nothing. Provided for compatibility.\n");
            Msg.add("SHELL_CMD_PATH_HELP","Provided for compatibility.\n");
            Msg.add("SHELL_CMD_VER_HELP","View and set the reported DOS version.\n");
            Msg.add("SHELL_CMD_VER_VER","DOSBox version %s. Reported DOS version %d.%02d.\n");

                /* Regular startup */
            call_shellstop=Callback.CALLBACK_Allocate();
            /* Setup the startup CS:IP to kill the last running machine when exitted */
            /*RealPt*/int newcsip=Callback.CALLBACK_RealPointer(call_shellstop);
            CPU_Regs.SegSet16CS(Memory.RealSeg(newcsip));
            CPU_Regs.reg_ip(Memory.RealOff(newcsip));

            Callback.CALLBACK_Setup(call_shellstop,shellstop_handler,Callback.CB_IRET,"shell stop");
            Program.PROGRAMS_MakeFile("COMMAND.COM",SHELL_ProgramStart);

            /* Now call up the shell for the first time */
            /*Bit16u*/int psp_seg= Dos.DOS_FIRST_SHELL;
            /*Bit16u*/int env_seg=Dos.DOS_FIRST_SHELL+19; //DOS_GetMemory(1+(4096/16))+1;
            /*Bit16u*/int stack_seg= Dos_tables.DOS_GetMemory(2048/16);
            CPU_Regs.SegSet16SS(stack_seg);
            CPU_Regs.reg_esp.word(2046);

            /* Set up int 24 and psp (Telarium games) */
            Memory.real_writeb(psp_seg+16+1,0,0xea);		/* far jmp */
            Memory.real_writed(psp_seg+16+1,1,Memory.real_readd(0,0x24*4));
            Memory.real_writed(0,0x24*4,(psp_seg<<16) | ((16+1)<<4));

            /* Set up int 23 to "int 20" in the psp. Fixes what.exe */
            Memory.real_writed(0,0x23*4,(psp_seg<<16));

            /* Setup MCBs */
            Dos_MCB pspmcb=new Dos_MCB((/*Bit16u*/int)(psp_seg-1));
            pspmcb.SetPSPSeg(psp_seg);	// MCB of the command shell psp
            pspmcb.SetSize(0x10+2);
            pspmcb.SetType((short)0x4d);
            Dos_MCB envmcb=new Dos_MCB((/*Bit16u*/int)(env_seg-1));
            envmcb.SetPSPSeg(psp_seg);	// MCB of the command shell environment
            envmcb.SetSize(Dos.DOS_MEM_START-env_seg);
            envmcb.SetType((short)0x4d);

            /* Setup environment */
            /*PhysPt*/int env_write=Memory.PhysMake(env_seg,0);
            Memory.MEM_BlockWrite(env_write,path_string,(/*Bitu*/int)(path_string.length()+1));
            env_write += path_string.length()+1;
            Memory.MEM_BlockWrite(env_write,comspec_string,(/*Bitu*/int)(comspec_string.length()+1));
            env_write += comspec_string.length()+1;
            Memory.mem_writeb(env_write++,0);
            Memory.mem_writew(env_write,1);
            env_write+=2;
            Memory.MEM_BlockWrite(env_write,full_name,(/*Bitu*/int)(full_name.length()+1));

            Dos_PSP psp=new Dos_PSP(psp_seg);
            psp.MakeNew(0);
            Dos.dos.psp(psp_seg);

            /* The start of the filetable in the psp must look like this:
             * 01 01 01 00 02
             * In order to achieve this: First open 2 files. Close the first and
             * duplicate the second (so the entries get 01) */
            /*Bit16u*/IntRef dummy=new IntRef(0);
            Dos_files.DOS_OpenFile("CON",Dos_files.OPEN_READWRITE,dummy);	/* STDIN  */
            Dos_files.DOS_OpenFile("CON",Dos_files.OPEN_READWRITE,dummy);	/* STDOUT */
            Dos_files.DOS_CloseFile(0);							/* Close STDIN */
            Dos_files.DOS_ForceDuplicateEntry(1,0);				/* "new" STDIN */
            Dos_files.DOS_ForceDuplicateEntry(1,2);				/* STDERR */
            Dos_files.DOS_OpenFile("CON",Dos_files.OPEN_READWRITE,dummy);	/* STDAUX */
            Dos_files.DOS_OpenFile("CON",Dos_files.OPEN_READWRITE,dummy);	/* STDPRN */

            psp.SetParent(psp_seg);
            /* Set the environment */
            psp.SetEnvironment(env_seg);
            /* Set the command line for the shell start up */
            byte[] tail=new byte[128];
            tail[0]=(byte)init_line.length();
            StringHelper.strcpy(tail, 1, init_line);
            Memory.MEM_BlockWrite(Memory.PhysMake(psp_seg,128),tail,128);

            /* Setup internal DOS Variables */
            Dos.dos.dta((int)Memory.RealMake(psp_seg,0x80));
            Dos.dos.psp(psp_seg);


            first_shell=SHELL_ProgramStart.call();
            first_shell.Run();
            first_shell = null;//Make clear that it shouldn't be used anymore
        }
    };
}
