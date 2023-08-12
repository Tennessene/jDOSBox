package jdos.misc;

import jdos.Dosbox;
import jdos.cpu.Callback;
import jdos.dos.Dos;
import jdos.dos.Dos_PSP;
import jdos.dos.Dos_files;
import jdos.dos.drives.Drive_virtual;
import jdos.hardware.Memory;
import jdos.misc.setup.CommandLine;
import jdos.misc.setup.Section;
import jdos.shell.Dos_shell;
import jdos.shell.Shell;
import jdos.util.FileIOFactory;
import jdos.util.IntRef;
import jdos.util.StringHelper;
import jdos.util.StringRef;

import java.util.Vector;

public abstract class Program {
    static private /*Bitu*/int call_program;

    /* This registers a file on the virtual drive and creates the correct structure for it*/

    private static final byte[] exe_block={
        (byte)0xbc,0x00,0x04,					//MOV SP,0x400 decrease stack size
        (byte)0xbb,0x40,0x00,					//MOV BX,0x040 for memory resize
        (byte)0xb4,0x4a,						//MOV AH,0x4A	Resize memory block
        (byte)0xcd,0x21,						//INT 0x21
    //pos 12 is callback number
        (byte)0xFE,0x38,0x00,0x00,			//CALLBack number
        (byte)0xb8,0x00,0x4c,					//Mov ax,4c00
        (byte)0xcd,0x21,						//INT 0x21
    };

    private static final int CB_POS=12;

    static public interface PROGRAMS_Main {
        public Program call();
    }

    static private Vector internal_progs = new Vector();

    public abstract void Run();
    static public void PROGRAMS_MakeFile(String name,PROGRAMS_Main main) {
        /*Bit8u*/byte[] comdata = new byte[32];
        System.arraycopy(exe_block, 0, comdata, 0, exe_block.length);
        comdata[CB_POS]=(byte)(call_program&0xff);
        comdata[CB_POS+1]=(byte)((call_program>>8)&0xff);

        /* Copy save the pointer in the vector and save it's index */
        if (internal_progs.size()>255) Log.exit("PROGRAMS_MakeFile program size too large ("+internal_progs.size()+")");
        /*Bit8u*/int index = internal_progs.size();
        internal_progs.addElement(main);
        comdata[exe_block.length] = (byte)(index & 0xFF);
        /*Bit32u*/int size=exe_block.length+1;
        Drive_virtual.VFILE_Register(name,comdata,size);
    }

    static private Callback.Handler PROGRAMS_Handler = new Callback.Handler() {
        public String getName() {
            return "Program.PROGRAMS_Handler";
        }
        public /*Bitu*/int call() {
            /* This sets up everything for a program start up call */
            /*Bitu*/int size=1/*sizeof(Bit8u)*/;
            /*Bit8u*/int index;
            /* Read the index from program code in memory */
            /*PhysPt*/int reader= Memory.PhysMake(Dos.dos.psp(),256+exe_block.length);
            index=Memory.mem_readb(reader++);
            Program new_program;
            if(index > internal_progs.size()) Log.exit("something is messing with the memory");
            PROGRAMS_Main handler = (PROGRAMS_Main)internal_progs.elementAt(index);
            new_program = handler.call();
            new_program.Run();
            return Callback.CBRET_NONE;
        }
    };

    /* Main functions used in all program */

    protected String temp_line;
    protected CommandLine cmd;
    protected Dos_PSP psp;

    public Program() {
        /* Find the command line and setup the PSP */
        psp = new Dos_PSP(Dos.dos.psp());
        /* Scan environment for filename */
        /*PhysPt*/int envscan=Memory.PhysMake(psp.GetEnvironment(),0);
        while (Memory.mem_readb(envscan)!=0) envscan+=Memory.mem_strlen(envscan)+1;
        envscan+=3;
        String tail;
        tail = Memory.MEM_BlockRead(Memory.PhysMake(Dos.dos.psp(),128),128);
        if (tail.length()>0)
            tail = tail.substring(1, (int)tail.charAt(0)+1);
        String filename = Memory.MEM_StrCopy(envscan,256);
        cmd = new CommandLine(filename,tail);
    }


    public void ChangeToLongCmd() {
    	/*
    	 * Get arguments directly from the shell instead of the psp.
    	 * this is done in securemode: (as then the arguments to mount and friends
    	 * can only be given on the shell ( so no int 21 4b)
    	 * Securemode part is disabled as each of the internal command has already
    	 * protection for it. (and it breaks games like cdman)
    	 * it is also done for long arguments to as it is convient (as the total commandline can be longer then 127 characters.
    	 * imgmount with lot's of parameters
    	 * Length of arguments can be ~120. but switch when above 100 to be sure
    	 */

    	if(/*control->SecureMode() ||*/ cmd.Get_arglength() > 100) {
    		CommandLine temp = new CommandLine(cmd.GetFileName(),Dos_shell.full_arguments);
    		cmd = temp;
    	}
    	Dos_shell.full_arguments=""; //Clear so it gets even more save
    }
    static byte last_written_character = 0;//For 0xA to OxD 0xA expansion
    protected void WriteOut(String format) {
        WriteOut(format, new Object[0]);
    }
    public void WriteOut(String format, Object[] args) {
        String buf = StringHelper.sprintf(format, args);
    	/*Bit16u*/int size = buf.length();
    	for(/*Bit16u*/int i = 0; i < size;i++) {
    		/*Bit8u*/byte[] out=new byte[1];/*Bit16u*/IntRef s=new IntRef(1);
    		if(buf.charAt(i) == 0xA && last_written_character != 0xD) {
    			out[0] = 0xD;Dos_files.DOS_WriteFile(Dos_files.STDOUT,out,s);
    		}
    		last_written_character =out[0] = (byte)buf.charAt(i);
    		Dos_files.DOS_WriteFile(Dos_files.STDOUT,out,s);
    	}

    //	DOS_WriteFile(STDOUT,(Bit8u *)buf,&size);
    }

    protected void WriteOut_NoParsing(String format) {
        /*Bit16u*/int size = format.length();
    	for(/*Bit16u*/int i = 0; i < size;i++) {
    		/*Bit8u*/byte[] out=new byte[1];/*Bit16u*/IntRef s=new IntRef(1);
    		if(format.charAt(i) == 0xA && last_written_character != 0xD) {
    			out[0] = 0xD;Dos_files.DOS_WriteFile(Dos_files.STDOUT,out,s);
    		}
    		last_written_character =out[0] = (byte)format.charAt(i);
    		Dos_files.DOS_WriteFile(Dos_files.STDOUT,out,s);
    	}

    //	DOS_WriteFile(STDOUT,(Bit8u *)format,&size);
    }

    public boolean GetEnvStr(String entry, StringRef result) {
        if (entry.equalsIgnoreCase("errorlevel")) {
            result.value = entry+"="+String.valueOf(Dos.dos.return_code);
            return true;
        }
    	/* Walk through the internal environment and see for a match */
    	/*PhysPt*/int env_read=Memory.PhysMake(psp.GetEnvironment(),0);

    	String env_string;
    	result.value="";
    	if (entry.length()==0) return false;
    	do 	{
    		env_string=Memory.MEM_StrCopy(env_read,1024);
    		if (env_string.length()==0) return false;
    		env_read += env_string.length()+1;
            int pos = env_string.indexOf('=');
    		if (pos<0) continue;
            String key = env_string.substring(0,pos);
            if (key.equalsIgnoreCase(entry)) {
                result.value = env_string;
                return true;
            }
    	} while (true);
    }

    public boolean GetEnvNum(/*Bitu*/int num,StringRef result) {
    	String env_string;
    	/*PhysPt*/int env_read=Memory.PhysMake(psp.GetEnvironment(),0);
    	do 	{
    		env_string=Memory.MEM_StrCopy(env_read,1024);
    		if (env_string.length()==0) return false;
    		if (num==0) { result.value=env_string;return true;}
    		env_read += env_string.length()+1;
    		num--;
    	} while (true);
    }

    public /*Bitu*/int GetEnvCount() {
    	/*PhysPt*/int env_read=Memory.PhysMake(psp.GetEnvironment(),0);
        /*Bitu*/int num=0;
    	while (Memory.mem_readb(env_read)!=0) {
    		for (;Memory.mem_readb(env_read)!=0;env_read++) {};
    		env_read++;
    		num++;
    	}
        return num;
    }

    public boolean SetEnv(String entry,String new_string) {
    	/*PhysPt*/int env_read=Memory.PhysMake(psp.GetEnvironment(),0);
    	/*PhysPt*/int env_write=env_read;
    	String env_string;
    	do 	{
    		env_string = Memory.MEM_StrCopy(env_read,1024);
    		if (env_string.length()==0) break;
    		env_read += env_string.length()+1;
            int pos = env_string.indexOf('=');
    		if (pos<0) continue; /* Remove corrupt entry? */
            String key = env_string.substring(0,pos);
            if (key.equalsIgnoreCase(entry)) {
                continue;
            }
    		Memory.MEM_BlockWrite(env_write,env_string,env_string.length()+1);
    		env_write += env_string.length()+1;
    	} while (true);
    /* TODO Maybe save the program name sometime. not really needed though */
    	/* Save the new entry */
    	if (new_string.length()>0) {
            new_string=entry.toUpperCase()+"="+new_string;
    		Memory.MEM_BlockWrite(env_write,new_string,new_string.length()+1);
    		env_write += new_string.length()+1;
    	}
    	/* Clear out the final piece of the environment */
    	Memory.mem_writed(env_write,0);
        return true;
    }

    static class CONFIG extends Program {
        public void Run() {
            if ((temp_line=cmd.FindString("-writeconf",true))!=null || (temp_line=cmd.FindString("-wc",true))!=null) {
                /* In secure mode don't allow a new configfile to be created */
                if(Dosbox.control.SecureMode()) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                    return;
                }
                if (!FileIOFactory.canOpen(temp_line, FileIOFactory.MODE_WRITE)) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_FILE_ERROR"),new Object[] {temp_line});
                    return;
                }
                Dosbox.control.PrintConfig(temp_line);
                return;
            }
            if ((temp_line=cmd.FindString("-writelang",true))!=null || (temp_line=cmd.FindString("-wl",true))!=null) {
                /* In secure mode don't allow a new languagefile to be created
                 * Who knows which kind of file we would overwriting. */
                if(Dosbox.control.SecureMode()) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                    return;
                }
                if (!FileIOFactory.canOpen(temp_line, FileIOFactory.MODE_WRITE)) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_FILE_ERROR"),new Object[] {temp_line});
                    return;
                }
                Msg.write(temp_line);
                return;
            }

            /* Code for switching to secure mode */
            if(cmd.FindExist("-securemode",true)) {
                Dosbox.control.SwitchToSecureMode();
                WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_ON"));
                return;
            }

            /* Code for getting the current configuration.           *
             * Official format: config -get "section property"       *
             * As a bonus it will set %CONFIG% to this value as well */
            if((temp_line=cmd.FindString("-get",true))!=null) {
                String temp2 = cmd.GetStringRemain();//So -get n1 n2= can be used without quotes
                if(temp2 != null && temp2.length()>0) temp_line = temp_line + " " + temp2;

                int space = temp_line.indexOf(" ");
                if(space<0) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_GET_SYNTAX"));
                    return;
                }
                //Copy the found property to a new string and erase from templine (mind the space)
                String prop = temp_line.substring(space+1); temp_line = temp_line.substring(0, space);

                Section sec = Dosbox.control.GetSection(temp_line);
                if(sec==null) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_SECTION_ERROR"),new Object[] {temp_line});
                    return;
                }
                String val = sec.GetPropValue(prop);
                if(val.equals(Section.NO_SUCH_PROPERTY)) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_NO_PROPERTY"), new Object[] {prop,temp_line});
                    return;
                }
                WriteOut(val);
                Shell.first_shell.SetEnv("CONFIG",val);
                return;
            }



            /* Code for the configuration changes                                  *
             * Official format: config -set "section property=value"               *
             * Accepted: without quotes and/or without -set and/or without section *
             *           and/or the "=" replaced by a " "                          */

            if ((temp_line=cmd.FindString("-set",true))!=null) { //get all arguments
                String temp2 = cmd.GetStringRemain();//So -set n1 n2=n3 can be used without quotes
                if(temp2!=null && temp2.length()>0) temp_line = temp_line + " " + temp2;
            } else 	if((temp_line=cmd.GetStringRemain())==null) {//no set
                WriteOut(Msg.get("PROGRAM_CONFIG_USAGE")); //and no arguments specified
                return;
            }
            //Wanted input: n1 n2=n3
            //seperate section from property
            int pos = temp_line.indexOf(' ');
            if (pos<0)
                pos = temp_line.indexOf('=');
            String copy;
            if (pos>=0) {
                copy = temp_line.substring(0, pos);
                temp_line=temp_line.substring(pos+1);
            } else {
                WriteOut(Msg.get("PROGRAM_CONFIG_USAGE"));
                return;
            }
            //if n1 n2 n3 then replace last space with =
            int sign = temp_line.indexOf('=');
            if(sign<=0) {
                sign = temp_line.indexOf(' ');
                if(sign>=0) {
                    temp_line = temp_line.substring(0, sign)+"="+temp_line.substring(sign+1);
                } else {
                    //2 items specified (no space nor = between n2 and n3
                    //assume that they posted: property value
                    //Try to determine the section.
                    Section sec=Dosbox.control.GetSectionFromProperty(copy);
                    if(sec==null){
                        if(Dosbox.control.GetSectionFromProperty(temp_line)!=null) return; //Weird situation:ignore
                        WriteOut(Msg.get("PROGRAM_CONFIG_PROPERTY_ERROR"),new Object[] {copy});
                        return;
                    } //Hack to allow config ems true
                    temp_line = copy+"="+temp_line;
                    copy = sec.GetName();
                    sign = temp_line.indexOf(' ');
                    if(sign>=0) 
                        temp_line = temp_line.substring(0, sign)+"="+temp_line.substring(sign+1);
                }
            }

            /* Input processed. Now the real job starts
             * copy contains the likely "sectionname"
             * temp contains "property=value"
             * the section is destroyed and a new input line is given to
             * the configuration parser. Then the section is restarted.
             */
            Section sec = Dosbox.control.GetSection(copy);
            if(sec==null) { WriteOut(Msg.get("PROGRAM_CONFIG_SECTION_ERROR"),new Object[] {copy});return;}
            sec.ExecuteDestroy(false);
            sec.HandleInputline(temp_line);
            sec.ExecuteInit(false);
        }
    }

    static private PROGRAMS_Main CONFIG_ProgramStart = new PROGRAMS_Main() {
        public Program call() {
            return new CONFIG();
        }
    };

    public static Section.SectionFunction PROGRAMS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            call_program=Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_program,PROGRAMS_Handler,Callback.CB_RETF,"internal program");
            PROGRAMS_MakeFile("CONFIG.COM",CONFIG_ProgramStart);

            Msg.add("PROGRAM_CONFIG_FILE_ERROR","Can't open file %s\n");
            Msg.add("PROGRAM_CONFIG_USAGE","Config tool:\nUse -writeconf filename to write the current config.\nUse -writelang filename to write the current language strings.\n");
            Msg.add("PROGRAM_CONFIG_SECURE_ON","Switched to secure mode.\n");
            Msg.add("PROGRAM_CONFIG_SECURE_DISALLOW","This operation is not permitted in secure mode.\n");
            Msg.add("PROGRAM_CONFIG_SECTION_ERROR","Section %s doesn't exist.\n");
            Msg.add("PROGRAM_CONFIG_PROPERTY_ERROR","No such section or property.\n");
            Msg.add("PROGRAM_CONFIG_NO_PROPERTY","There is no property %s in section %s.\n");
            Msg.add("PROGRAM_CONFIG_GET_SYNTAX","Correct syntax: config -get \"section property\".\n");
        }
    };
}
