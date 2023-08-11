package jdos.shell;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.*;
import jdos.dos.drives.Drive_local;
import jdos.hardware.Memory;
import jdos.ints.Bios;
import jdos.ints.Bios_keyboard;
import jdos.misc.Log;
import jdos.misc.Msg;
import jdos.misc.Program;
import jdos.misc.setup.CommandLine;
import jdos.misc.setup.Config;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.*;

import java.util.Calendar;
import java.util.Vector;

public class Dos_shell extends Program {
    public interface handler {
        public void call(String arg);
    }

    private Vector l_history = new Vector();
    private Vector l_completion = new Vector();

    /* The shell's variables */
    /*Bit16u*/int input_handle;
    public BatchFile bf;
    boolean echo;
    boolean exit;
    boolean call;

    String completion_start;
    /*Bit16u*/int completion_index;

    public Dos_shell() {
        input_handle= Dos_files.STDIN;
        echo=true;
        exit=false;
        bf=null;
        call=false;
        completion_start = null;
    }

    public void Run() {
        StringRef line = new StringRef();
        if (cmd.FindStringRemainBegin("/C", line)) {

            //GTA installer
            int pos = line.value.indexOf('\n');
            if (pos>=0) line.value = line.value.substring(0, pos);
            pos = line.value.indexOf('\r');
            if (pos>=0) line.value = line.value.substring(0, pos);

            Dos_shell temp = new Dos_shell();
            temp.echo = echo;
            temp.ParseLine(line.value);		//for *.exe *.com  |*.bat creates the bf needed by runinternal;
            temp.RunInternal();				// exits when no bf is found.
            return;
        }
        //:TODO:/* Start a normal shell and check for a first command init */
        WriteOut(Msg.get("SHELL_STARTUP_BEGIN"), new Object[] {Config.VERSION});
        if (Config.C_DEBUG)
            WriteOut(Msg.get("SHELL_STARTUP_DEBUG"));
        if (Dosbox.machine == MachineType.MCH_CGA) WriteOut(Msg.get("SHELL_STARTUP_CGA"));
        if (Dosbox.machine == MachineType.MCH_HERC) WriteOut(Msg.get("SHELL_STARTUP_HERC"));
        WriteOut(Msg.get("SHELL_STARTUP_END"));

        if ((line.value=cmd.FindString("/INIT",true))!=null) {
            ParseLine(line.value);
        }
        do {
            if (bf!=null){
                String input_line;
                if((input_line=bf.ReadLine())!=null) {
                    if (echo) {
                        if (input_line.length()>0 && input_line.charAt(0)!='@') {
                            ShowPrompt();
                            WriteOut_NoParsing(input_line);
                            WriteOut_NoParsing("\n");
                        }
                    }
                    ParseLine(input_line);
                    if (echo) WriteOut("\n");
                }
            } else {
                if (echo) ShowPrompt();
                String input_line = InputCommand();
                if (input_line==null) input_line="";
                ParseLine(input_line);
                if (echo && bf==null) WriteOut_NoParsing("\n");
            }
        } while (!exit);
    }

    void RunInternal() { //for command /C
        String input_line;
        while(bf!=null && (input_line=bf.ReadLine())!=null)
        {
            if (echo) {
                if (input_line.charAt(0) != '@') {
                    ShowPrompt();
                    WriteOut_NoParsing(input_line);
                    WriteOut_NoParsing("\n");
                }
            }
            ParseLine(input_line);
        }
    }

/* A load of subfunctions */
    void ParseLine(String line) {
        Log.log(LogTypes.LOG_EXEC, LogSeverities.LOG_ERROR,"Parsing command line: "+line);
        /* Check for a leading @ */
        if (line.startsWith("@")) line = line.substring(1);
        line = line.trim();

        /* Do redirection and pipe checks */

        StringRef in  = new StringRef(null);
        StringRef out  = new StringRef(null);

        /*Bit16u*/IntRef dummy=new IntRef(0),dummy2=new IntRef(0);
        /*Bit32u*/LongRef bigdummy = new LongRef(0);
        /*Bitu*/int num = 0;		/* Number of commands in this line */
        BooleanRef append = new BooleanRef();
        boolean normalstdin  = false;	/* wether stdin/out are open on start. */
        boolean normalstdout = false;	/* Bug: Assumed is they are "con"      */
        StringRef s = new StringRef(line);
        num = GetRedirection(s, in, out, append);
        line = s.value;
        if (num>1) Log.log_msg("SHELL:Multiple command on 1 line not supported");
        if (in.value!=null || out!=null) {
            normalstdin  = (psp.GetFileHandle(0) != 0xff);
            normalstdout = (psp.GetFileHandle(1) != 0xff);
        }
        if (in.value != null) {
            if(Dos_files.DOS_OpenFile(in.value,Dos_files.OPEN_READ,dummy)) {	//Test if file exists
                Dos_files.DOS_CloseFile(dummy.value);
                Log.log_msg("SHELL:Redirect input from "+in);
                if(normalstdin) Dos_files.DOS_CloseFile(0);	//Close stdin
                Dos_files.DOS_OpenFile(in.value,Dos_files.OPEN_READ,dummy);	//Open new stdin
            }
        }
        if (out.value != null){
            Log.log_msg("SHELL:Redirect output to "+out.value);
            if(normalstdout) Dos_files.DOS_CloseFile(1);
            if(!normalstdin && in.value == null) Dos_files.DOS_OpenFile("con",Dos_files.OPEN_READWRITE,dummy);
            boolean status = true;
            /* Create if not exist. Open if exist. Both in read/write mode */
            if(append.value) {
                if( (status = Dos_files.DOS_OpenFile(out.value,Dos_files.OPEN_READWRITE,dummy)) ) {
                     Dos_files.DOS_SeekFile(1,bigdummy,Dos_files.DOS_SEEK_END);
                } else {
                    status = Dos_files.DOS_CreateFile(out.value, Dos_system.DOS_ATTR_ARCHIVE,dummy);	//Create if not exists.
                }
            } else {
                status = Dos_files.DOS_OpenFileExtended(out.value,Dos_files.OPEN_READWRITE,Dos_system.DOS_ATTR_ARCHIVE,0x12,dummy,dummy2);
            }

            if(!status && normalstdout) Dos_files.DOS_OpenFile("con",Dos_files.OPEN_READWRITE,dummy); //Read only file, open con again
            if(!normalstdin && in.value == null) Dos_files.DOS_CloseFile(0);
        }
        /* Run the actual command */
        DoCommand(line);
        /* Restore handles */
        if(in.value!=null) {
            Dos_files.DOS_CloseFile(0);
            if(normalstdin) Dos_files.DOS_OpenFile("con",Dos_files.OPEN_READWRITE,dummy);
        }
        if(out.value != null) {
            Dos_files.DOS_CloseFile(1);
            if(!normalstdin) Dos_files.DOS_OpenFile("con",Dos_files.OPEN_READWRITE,dummy);
            if(normalstdout) Dos_files.DOS_OpenFile("con",Dos_files.OPEN_READWRITE,dummy);
            if(!normalstdin) Dos_files.DOS_CloseFile(0);
        }
    }

    /*Bitu*/int GetRedirection(StringRef s, StringRef ifn, StringRef ofn, BooleanRef append) {
        String lr=s.value;
        StringBuffer lw=new StringBuffer();
        char ch;
        /*Bitu*/int num=0;
        boolean quote = false;

        while ( lr.length()>0 ) {
            ch = lr.charAt(0);
            lr = lr.substring(1);
            if(quote && ch != '"') { /* don't parse redirection within quotes. Not perfect yet. Escaped quotes will mess the count up */
                lw.append(ch);
                continue;
            }

            switch (ch) {
            case '"':
                quote = !quote;
                break;
            case '>':
                append.value=(lr.length()>0 && lr.charAt(0)=='>');
                if (append.value) lr = lr.substring(1);
                lr=lr.trim();
                ofn.value=lr;
                while (lr.length()>0 && lr.charAt(0)!=' ' && lr.charAt(0)!='<' && lr.charAt(0)!='|') lr = lr.substring(1);
                //if it ends on a : => remove it.
                if((ofn.value.length() != lr.length()) && ofn.value.endsWith(":")) ofn.value = ofn.value.substring(0, ofn.value.length()-1);
    //			if(*lr && *(lr+1))
    //				*lr++=0;
    //			else
    //				*lr=0;
                ofn.value = ofn.value.substring(0, ofn.value.length()-lr.length());
                continue;
            case '<':
                lr=lr.trim();
                ifn.value=lr;
                while (lr.length()>0 && lr.charAt(0)!=' ' && lr.charAt(0)!='<' && lr.charAt(0)!='|') lr = lr.substring(1);
                //if it ends on a : => remove it.
                if((ifn.value.length() != lr.length()) && ifn.value.endsWith(":")) ifn.value = ifn.value.substring(0, ifn.value.length()-1);
    //			if(*lr && *(lr+1))
    //				*lr++=0;
    //			else
    //				*lr=0;
                ifn.value = ifn.value.substring(0, ifn.value.length()-lr.length());
                continue;
            case '|':
                ch=0;
                num++;
            }
            lw.append(ch);
        }
        s.value = lw.toString();
        return num;
    }

    static void outc(int b) {
        byte[] c = new byte[1];
        c[0]=(byte)b;
	    /*Bit16u*/IntRef n=new IntRef(1);
	    Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
    }

    String InputCommand() {
        /*Bitu*/int size=Shell.CMD_MAXLINE-2; //lastcharacter+0
        /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
        /*Bitu*/int str_len=0;/*Bitu*/int str_index=0;
        /*Bit16u*/IntRef len=new IntRef(0);
        boolean current_hist=false; // current command stored in history?
        byte[] line=new byte[Shell.CMD_MAXLINE];

        int it_history = 0;
        int it_completion = 0;

        while (size!=0) {
            Dos.dos.echo=false;
            while(!Dos_files.DOS_ReadFile(input_handle,c,n)) {
                /*Bit16u*/IntRef dummy=new IntRef(0);
                Dos_files.DOS_CloseFile(input_handle);
                Dos_files.DOS_OpenFile("con",2,dummy);
                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_ERROR,"Reopening the input handle. This is a bug!");
            }
            if (n.value == 0) {
                size=0;			//Kill the while loop
                continue;
            }
            switch (c[0]) {
            case 0x00:				/* Extended Keys */
                {
                    Dos_files.DOS_ReadFile(input_handle,c,n);
                    switch (c[0]) {

                    case 0x3d:		/* F3 */
                        if (l_history.size()==0) break;
                        it_history = 0;
                        if (l_history.size()>0 && ((String)l_history.firstElement()).length() > str_len) {
                            String reader = ((String)l_history.firstElement()).substring(str_len);
                            for (int i=0;i<reader.length();i++) {
                                c[0]=(byte)reader.charAt(0);
                                line[str_index ++] = (byte)reader.charAt(0);
                                Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                            }
                            str_len = str_index = ((String)l_history.firstElement()).length();
                            size = Shell.CMD_MAXLINE - str_index - 2;
                            line[str_len] = 0;
                        }
                        break;

                    case 0x4B:	/* LEFT */
                        if (str_index!=0) {
                            outc(8);
                            str_index --;
                        }
                        break;

                    case 0x4D:	/* RIGHT */
                        if (str_index < str_len) {
                            outc(line[str_index++]);
                        }
                        break;

                    case 0x47:	/* HOME */
                        while (str_index!=0) {
                            outc(8);
                            str_index--;
                        }
                        break;

                    case 0x4F:	/* END */
                        while (str_index < str_len) {
                            outc(line[str_index++]);
                        }
                        break;

                    case 0x48:	/* UP */
                        if (l_history.size()==0 || it_history == l_history.size()) break;

                        // store current command in history if we are at beginning
                        if (it_history == 0 && !current_hist) {
                            current_hist=true;
                            l_history.insertElementAt(new String(line, 0, str_len), 0);
                            it_history++;
                        }

                        for (;str_index>0; str_index--) {
                            // removes all characters
                            outc(8); outc(' '); outc(8);
                        }
                        StringHelper.strcpy(line, (String)l_history.elementAt(it_history));
                        len.value = ((String)l_history.elementAt(it_history)).length();
                        str_len = str_index = len.value;
                        size = Shell.CMD_MAXLINE - str_index - 2;
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT, line, len);
                        it_history ++;
                        break;

                    case 0x50:	/* DOWN */
                        if (l_history.size()==0 || it_history == 0) break;

                        // not very nice but works ..
                        it_history --;
                        if (it_history == 0) {
                            // no previous commands in history
                            it_history ++;

                            // remove current command from history
                            if (current_hist) {
                                current_hist=false;
                                l_history.removeElementAt(0);
                            }
                            break;
                        } else it_history --;

                        for (;str_index>0; str_index--) {
                            // removes all characters
                            outc(8); outc(' '); outc(8);
                        }
                        StringHelper.strcpy(line, (String)l_history.elementAt(it_history));
                        len.value = ((String)l_history.elementAt(it_history)).length();
                        str_len = str_index = len.value;
                        size = Shell.CMD_MAXLINE - str_index - 2;
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT, line, len);
                        it_history ++;

                        break;
                    case 0x53:/* DELETE */
                        {
                            if(str_index>=str_len) break;
                            /*Bit16u*/IntRef a=new IntRef(str_len-str_index-1);
                            /*Bit8u*/byte[] text=new byte[a.value];
                            System.arraycopy(line, str_index+1, text, 0, a.value);
                            Dos_files.DOS_WriteFile(Dos_files.STDOUT,text,a);//write buffer to screen
                            outc(' ');outc(8);
                            for(/*Bitu*/int i=str_index;i<str_len-1;i++) {
                                line[i]=line[i+1];
                                outc(8);
                            }
                            line[--str_len]=0;
                            size++;
                        }
                        break;
                    case 15:		/* Shift-Tab */
                        if (l_completion.size()!=0) {
                            if (it_completion == 0) it_completion = l_completion.size();
                            it_completion--;
                            String s = (String)l_completion.elementAt(it_completion);
                            if (s.length()!=0) {
                                for (;str_index > completion_index; str_index--) {
                                    // removes all characters
                                    outc(8); outc(' '); outc(8);
                                }

                                StringHelper.strcpy(line, completion_index, s);
                                len.value = s.length();
                                str_len = str_index = completion_index + len.value;
                                size = Shell.CMD_MAXLINE - str_index - 2;
                                Dos_files.DOS_WriteFile(Dos_files.STDOUT, s.getBytes(), len);
                            }
                        }
                        break;
                    default:
                        break;
                    }
                }
                break;
            case 0x08:				/* BackSpace */
                if (str_index!=0) {
                    outc(8);
                    /*Bit32u*/int str_remain=str_len - str_index;
                    size++;
                    if (str_remain!=0) {
                        for (int i=0;i<str_remain;i++) line[str_index-1+i] = line[str_index+i];
                        line[--str_len]=0;
                        str_index --;
                        /* Go back to redraw */
                        for (/*Bit16u*/int i=str_index; i < str_len; i++)
                            outc(line[i]);
                    } else {
                        line[--str_index] = '\0';
                        str_len--;
                    }
                    outc(' ');	outc(8);
                    // moves the cursor left
                    while (str_remain--!=0) outc(8);
                }
                if (l_completion.size()!=0) l_completion.clear();
                break;
            case 0x0a:				/* New Line not handled */
                /* Don't care */
                break;
            case 0x0d:				/* Return */
                outc('\n');
                size=0;			//Kill the while loop
                break;
            case'\t':
                {
                    if (l_completion.size()!=0) {
                        it_completion ++;
                        if (it_completion == l_completion.size()) it_completion = 0;
                    } else {
                        // build new completion list
                        // Lines starting with CD will only get directories in the list
                        boolean dir_only = StringHelper.toString(line).toUpperCase().startsWith("CD ");

                        // get completion mask
                        String sLine = StringHelper.toString(line);
                        int p_completion_start = sLine.lastIndexOf(' ');

                        if (p_completion_start>=0) {
                            p_completion_start ++;
                            completion_index = p_completion_start;
                        } else {
                            p_completion_start = 0;
                            completion_index = 0;
                        }

                        int path;
                        if ((path = sLine.substring(completion_index).lastIndexOf('\\'))>=0) completion_index+=path+1;
                        if ((path = sLine.substring(completion_index).lastIndexOf('/'))>=0) completion_index+=path+1;

                        // build the completion list
                        String mask;
                        if (p_completion_start>=0) {
                            mask=sLine.substring(p_completion_start);
                            int dot_pos=mask.lastIndexOf('.');
                            int bs_pos=mask.lastIndexOf('\\');
                            int fs_pos=mask.lastIndexOf('/');
                            int cl_pos=mask.lastIndexOf(':');
                            // not perfect when line already contains wildcards, but works
                            if ((dot_pos-bs_pos>0) && (dot_pos-fs_pos>0) && (dot_pos-cl_pos>0))
                                mask+= "*";
                            else mask+= "*.*";
                        } else {
                            mask="*.*";
                        }

                        /*RealPt*/int save_dta=Dos.dos.dta();
                        Dos.dos.dta((int)Dos.dos.tables.tempdta);

                        boolean res = Dos_files.DOS_FindFirst(mask, 0xffff & ~Dos_system.DOS_ATTR_VOLUME);
                        if (!res) {
                            Dos.dos.dta((int)save_dta);
                            break;	// TODO: beep
                        }

                        Dos_DTA dta=new Dos_DTA(Dos.dos.dta());
                        StringRef name = new StringRef();
                        LongRef sz = new LongRef(0);
                        IntRef date = new IntRef(0);
                        IntRef time = new IntRef(0);
                        ShortRef att = new ShortRef(0);
                        int extIndex=0;
                        while (res) {
                            dta.GetResult(name,sz,date,time,att);
                            // add result to completion list

                            if (!name.value.equals(".") && !name.value.equals("..")) {
                                if (dir_only) { //Handle the dir only case different (line starts with cd)
                                    if((att.value & Dos_system.DOS_ATTR_DIRECTORY)!=0) l_completion.add(name.value);
                                } else {
                                    int pos = name.value.lastIndexOf('.');
                                    String ext = null;
                                    if (pos>=0)
                                        ext = name.value.substring(pos+1);
                                    if (ext!=null && (ext.equalsIgnoreCase("BAT") || ext.equalsIgnoreCase("COM") || ext.equalsIgnoreCase("EXE")))
                                        l_completion.insertElementAt(name.value,extIndex++);
                                    else
                                        l_completion.add(name.value);
                                }
                            }
                            res=Dos_files.DOS_FindNext();
                        }
                        it_completion = 0;
                        Dos.dos.dta((int)save_dta);
                    }

                    if (l_completion.size()!=0 && ((String)l_completion.elementAt(it_completion)).length()!=0) {
                        for (;str_index > completion_index; str_index--) {
                            // removes all characters
                            outc(8); outc(' '); outc(8);
                        }

                        StringHelper.strcpy(line,completion_index, (String)l_completion.elementAt(it_completion));
                        len.value = ((String)l_completion.elementAt(it_completion)).length();
                        str_len = str_index = completion_index + len.value;
                        size = Shell.CMD_MAXLINE - str_index - 2;
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT, ((String)l_completion.elementAt(it_completion)).getBytes(), len);
                    }
                }
                break;
            case 0x1b:   /* ESC */
                //write a backslash and return to the next line
                outc('\\');
                outc('\n');
                line[0] = 0;      // reset the line.
                if (l_completion.size()!=0) l_completion.clear(); //reset the completion list.
                StringHelper.strcpy(line, InputCommand());	//Get the NEW line.
                size = 0;       // stop the next loop
                str_len = 0;    // prevent multiple adds of the same line
                break;
            default:
                if (l_completion.size()!=0) l_completion.clear();
                if(str_index < str_len && true) { //mem_readb(BIOS_KEYBOARD_FLAGS1)&0x80) dev_con.h ?
                    outc(' ');//move cursor one to the right.
                    /*Bit16u*/IntRef a = new IntRef(str_len - str_index);
                    /*Bit8u*/byte[] text=new byte[a.value];
                    System.arraycopy(line, str_index, text, 0, a.value);
                    Dos_files.DOS_WriteFile(Dos_files.STDOUT,text,a);//write buffer to screen
                    outc(8);//undo the cursor the right.
                    for(/*Bitu*/int i=str_len;i>str_index;i--) {
                        line[i]=line[i-1]; //move internal buffer
                        outc(8); //move cursor back (from write buffer to screen)
                    }
                    line[++str_len]=0;//new end (as the internal buffer moved one place to the right
                    size--;
                }

                line[str_index]=c[0];
                str_index ++;
                if (str_index > str_len){
                    line[str_index] = '\0';
                    str_len++;
                    size--;
                }
                Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                break;
            }
        }

        if (str_len==0) return null;
        str_len++;

        // remove current command from history if it's there
        if (current_hist) {
            current_hist=false;
            l_history.removeElementAt(0);
        }

        // add command line to history
        String sLine = StringHelper.toString(line);
        l_history.insertElementAt(sLine, 0); it_history = 0;
        if (l_completion.size()!=0) l_completion.clear();
        return sLine;
    }

    void ShowPrompt() {
        char drive=(char)(Dos_files.DOS_GetDefaultDrive()+'A');
        StringRef dir = new StringRef();
        Dos_files.DOS_GetCurrentDir((short)0,dir);
        WriteOut(String.valueOf((char)drive)+":\\"+dir.value+">");
    }

    void DoCommand(String line) {
        /* First split the line into command and arguments */
        line = line.trim();
        StringBuffer cmd_buffer=new StringBuffer();
        while (line.length()>0) {
            if (line.charAt(0)==32) break;
            if (line.charAt(0)=='/') break;
            if (line.charAt(0)=='\t') break;
            if (line.charAt(0)=='=') break;
            //if (line.charAt(0)==':') break; //This breaks drive switching as that is handled at a later stage.
            if ((line.charAt(0)=='.') ||(line.charAt(0) =='\\')) {  //allow stuff like cd.. and dir.exe cd\kees
                //cmd_buffer.append((char)0);
                /*Bit32u*/int cmd_index=0;
                while (cmd_list[cmd_index].name!=null) {
                    if (cmd_buffer.toString().equalsIgnoreCase(cmd_list[cmd_index].name)) {
                        cmd_list[cmd_index].handler.call(line);
                        return;
                    }
                    cmd_index++;
                }
            }
            cmd_buffer.append(line.charAt(0));
            line = line.substring(1);
        }
        if (cmd_buffer.length()==0) return;
    /* Check the internal list */
        /*Bit32u*/int cmd_index=0;
        while (cmd_list[cmd_index].name!=null) {
            if (cmd_buffer.toString().equalsIgnoreCase(cmd_list[cmd_index].name)) {
                cmd_list[cmd_index].handler.call(line);
                return;
            }
            cmd_index++;
        }
    /* This isn't an internal command execute it */
        if(Execute(cmd_buffer.toString(),line)) return;
        if(CheckConfig(cmd_buffer.toString(),line)) return;
        WriteOut(Msg.get("SHELL_EXECUTE_ILLEGAL_COMMAND"),new Object[] {cmd_buffer.toString()});
    }

    public static String full_arguments;
    public boolean Execute(String name,String args) {
    /* return true  => don't check for hardware changes in do_command
     * return false =>       check for hardware changes in do_command */
        String fullname;
        String p_fullname;
        String line = args;
        if(args.length()!=0){
            if(args.charAt(0) != ' '){ //put a space in front
                line = ' '+line;
            }
        }

        /* check for a drive change */
        if ((name.substring(1).equals(":") || name.substring(1).equals(":\\")) && StringHelper.isalpha(name.charAt(0)))
        {
            if (!Dos_files.DOS_SetDrive((short)(name.toUpperCase().charAt(0)-'A'))) {
                WriteOut(Msg.get("SHELL_EXECUTE_DRIVE_NOT_FOUND"),new Object[] {new Character(name.toUpperCase().charAt(0))});
            }
            return true;
        }
        /* Check for a full name */
        p_fullname = Which(name);
        if (p_fullname==null) return false;
        fullname = p_fullname;
        int extension = fullname.lastIndexOf('.');
        String sExtension="";
        /*always disallow files without extension from being executed. */
        /*only internal commands can be run this way and they never get in this handler */
        if(extension >=0 ) {
            sExtension = fullname.substring(extension).toLowerCase();
        } else {
            String temp_name = fullname+".COM";
            String temp_fullname=Which(temp_name);
            if (temp_fullname!=null) { sExtension=".com";fullname=temp_fullname; }

            else
            {
                temp_name = fullname+".EXE";
                temp_fullname=Which(temp_name);
                temp_fullname=Which(temp_name);
                if (temp_fullname!=null) { sExtension=".exe";fullname=temp_fullname; }

                else
                {
                    temp_name = fullname+".BAT";
                    temp_fullname=Which(temp_name);
                    temp_fullname=Which(temp_name);
                    if (temp_fullname!=null) { sExtension=".bat";fullname=temp_fullname; }

                    else
                    {
                        return false;
                    }

                }
            }
        }

        if (sExtension.equalsIgnoreCase(".bat"))
        {	/* Run the .bat file */
            /* delete old batch file if call is not active*/
            boolean temp_echo=echo; /*keep the current echostate (as delete bf might change it )*/
            if(bf!=null && !call) bf.close();
            bf=new BatchFile(this,fullname,name,line);
            echo=temp_echo; //restore it.
        }
        else
        {	/* only .bat .exe .com extensions maybe be executed by the shell */
            if (!sExtension.equalsIgnoreCase(".com") && !sExtension.equalsIgnoreCase(".exe")) return false;
            /* Run the .exe or .com file from the shell */
            /* Allocate some stack space for tables in physical memory */
            CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word()-0x200);
            //Add Parameter block
            Dos_ParamBlock block=new Dos_ParamBlock(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word());
            block.Clear();
            //Add a filename
            /*RealPt*/int file_name= CPU_Regs.RealMakeSegSS(CPU_Regs.reg_esp.word()+0x20);
            Memory.MEM_BlockWrite(Memory.Real2Phys(file_name),fullname,fullname.length()+1);

            /* HACK: Store full commandline for mount and imgmount */
            full_arguments=line;

            /* Fill the command line */
            byte[] cmdtail = new byte[128];
            cmdtail[0]=(byte)line.length();
            StringHelper.strcpy(cmdtail, 1, line);
            cmdtail[line.length()+1]=0xd;
            /* Copy command line in stack block too */
            Memory.MEM_BlockWrite(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.word()+0x100,cmdtail,128);
            /* Parse FCB (first two parameters) and put them into the current DOS_PSP */
            /*Bit8u*/ShortRef add=new ShortRef(0);
            String tailBuffer = StringHelper.toString(cmdtail, 1, cmdtail.length-1);
            Dos_files.FCB_Parsename(Dos.dos.psp(),0x5C,(short)0x00,tailBuffer,add);
            Dos_files.FCB_Parsename(Dos.dos.psp(),0x6C,(short)0x00,tailBuffer.substring(add.value),add);
            block.exec.fcb1=Memory.RealMake(Dos.dos.psp(),0x5C);
            block.exec.fcb2=Memory.RealMake(Dos.dos.psp(),0x6C);
            /* Set the command line in the block and save it */
            block.exec.cmdtail=CPU_Regs.RealMakeSegSS(CPU_Regs.reg_esp.word()+0x100);
            block.SaveData();
//    #if 0
//            /* Save CS:IP to some point where i can return them from */
//            Bit32u oldeip=reg_eip;
//            Bit16u oldcs=SegValue(cs);
//            RealPt newcsip=CALLBACK_RealPointer(call_shellstop);
//            SegSet16(cs,RealSeg(newcsip));
//            reg_ip=RealOff(newcsip);
//    #endif
            /* Start up a dos execute interrupt */
            CPU_Regs.reg_eax.word(0x4b00);
            //Filename pointer
            CPU_Regs.SegSet16DS((int)CPU_Regs.reg_ssVal.dword);
            CPU_Regs.reg_edx.word(Memory.RealOff(file_name));
            //Paramblock
            CPU_Regs.SegSet16ES((int)CPU_Regs.reg_ssVal.dword);
            CPU_Regs.reg_ebx.word(CPU_Regs.reg_esp.word());
            CPU_Regs.SETFLAGBIT(CPU_Regs.IF,false);
            Callback.CALLBACK_RunRealInt(0x21);
            /* Restore CS:IP and the stack */
            CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word()+0x200);
//    #if 0
//            reg_eip=oldeip;
//            SegSet16(cs,oldcs);
//    #endif
        }
        return true; //Executable started
    }

    /* Checks if it matches a hardware-property */
    boolean CheckConfig(String cmd_in,String line) {
        Section test = Dosbox.control.GetSectionFromProperty(cmd_in);
        if(test==null) return false;
        if(line!=null && line.length()==0) {
            String val = test.GetPropValue(cmd_in);
            if(!val.equals(Section.NO_SUCH_PROPERTY)) WriteOut(val+"\n");
            return true;
        }
        String newcom = "z:\\config "+test.GetName()+" "+cmd_in+line;
        DoCommand(newcom);
        return true;
    }

    String Which(String name) {
        /* Parse through the Path to find the correct entry */
        /* Check if name is already ok but just misses an extension */

        if (Dos_files.DOS_FileExists(name)) return name;
        /* try to find .com .exe .bat */
        if (Dos_files.DOS_FileExists(name+".COM")) return name+".COM";
        if (Dos_files.DOS_FileExists(name+".EXE")) return name+".EXE";
        if (Dos_files.DOS_FileExists(name+".BAT")) return name+".BAT";

        /* No Path in filename look through path environment string */
        StringRef temp = new StringRef();
        if (!GetEnvStr("PATH",temp)) return null;
        if (temp.value.length()==0) return null;
        int pos = temp.value.indexOf('=');
        if (pos<0) return null;
        String pathenv = temp.value.substring(pos+1);

        while (pathenv.length()>0) {
            /* remove ; and ;; at the beginning. (and from the second entry etc) */
            while(pathenv.length()>0 && pathenv.charAt(0) ==';')
                pathenv=pathenv.substring(1);

            /* get next entry */
            StringBuffer path = new StringBuffer();
            while(pathenv.length()>0 && pathenv.charAt(0) !=';') {
                path.append(pathenv.charAt(0));
                pathenv = pathenv.substring(1);
            }

            /* check entry */
            if(path.length()>0){
                if (path.charAt(path.length()-1)!='\\')
                    path.append("\\");
                path.append(name);
                String p = path.toString();

                if (Dos_files.DOS_FileExists(p)) return p;
                if (Dos_files.DOS_FileExists(p+".COM")) return p+".COM";
                if (Dos_files.DOS_FileExists(p+".EXE")) return p+".EXE";
                if (Dos_files.DOS_FileExists(p+".BAT")) return p+".BAT";
            }
        }
        return null;
    }

    private boolean HELP(StringRef args, String command) {
        if (ScanCMDBool(args,"?")) {
		    WriteOut(Msg.get("SHELL_CMD_"+command+"_HELP"));
		    String long_m = Msg.get("SHELL_CMD_" + command + "_HELP_LONG");
		    WriteOut("\n");
		    if(!long_m.equals("Message not Found!\n")) WriteOut(long_m);
		    else WriteOut(command+"\n");
		    return true;
        }
        return false;
	}

    static private String StripSpaces(String args) {
        while (args.length()>0 && StringHelper.isspace(args.charAt(0))) {
            args = args.substring(1);
        }
        return args;
    }

    static private String StripSpaces(String args,char also) {
        while (args.length()>0 && (StringHelper.isspace(args.charAt(0)) || args.charAt(0)==also)) {
            args = args.substring(1);
        }
        return args;
    }

    static String FormatNumber(/*Bitu*/long num) {
        /*Bitu*/long numm,numk,numb,numg;
        numb=num % 1000;
        num/=1000;
        numk=num % 1000;
        num/=1000;
        numm=num % 1000;
        num/=1000;
        numg=num;
        if (numg!=0) {
            return StringHelper.sprintf("%d,%03d,%03d,%03d",new Object[]{new Long(numg),new Long(numm),new Long(numk),new Long(numb)});
        }
        if (numm!=0) {
            return StringHelper.sprintf("%d,%03d,%03d",new Object[]{new Long(numm),new Long(numk),new Long(numb)});
        }
        if (numk!=0) {
            return StringHelper.sprintf("%d,%03d",new Object[]{new Long(numk),new Long(numb)});
        }
        return String.valueOf(numb);
    }

    private static String ExpandDot(StringRef args) {
        if(args.value.startsWith(".")) {
            if(args.value.length()==1){
                return "*.*";
            }
            if (args.value.charAt(1) != '.' && args.value.charAt(1) != '\\') {
                return "*"+args.value;
            }
        }
        return args.value;
    }

    static private boolean ScanCMDBool(StringRef cmd,String check) {
        int pos = 0;
        check = "/"+check;
        while ((pos = cmd.value.toUpperCase().indexOf(check.toUpperCase(),pos))>=0) {
            int start = pos;
            pos+=check.length();
            if (cmd.value.length()==pos || cmd.value.charAt(pos)==' ' || cmd.value.charAt(pos)=='\t' || cmd.value.charAt(pos)=='/') {
                cmd.value = cmd.value.substring(0, start) + cmd.value.substring(pos).trim();
                return true;
            }
        }
        return false;
    }

    static private String ScanCMDRemain(StringRef cmd) {
        int pos = cmd.value.indexOf('/');
        if (pos>=0) {
            String scan = cmd.value.substring(pos+1);
            StringBuffer found = new StringBuffer();
            while (scan.length()>0 && !StringHelper.isspace(scan.charAt(0))) {
                found.append(scan.charAt(0));
                scan = scan.substring(1);
            }
            return found.toString();
        } else return null;
    }

/* Some supported commands */
    handler CMD_HELP = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "HELP")) return;
            boolean optall=ScanCMDBool(args,"ALL");
            /* Print the help */
            if(!optall) WriteOut(Msg.get("SHELL_CMD_HELP"));
            /*Bit32u*/int cmd_index=0,write_count=0;
            while (cmd_list[cmd_index].name!=null) {
                if (optall || cmd_list[cmd_index].flags==0) {
                    WriteOut("<\033[34;1m"+StringHelper.leftJustify(cmd_list[cmd_index].name, 8)+"\033[0m> "+Msg.get(cmd_list[cmd_index].help));
                    if((++write_count%22)==0) CMD_PAUSE.call("");
                }
                cmd_index++;
            }
        }
    };

    handler CMD_CLS = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CLS")) return;
            CPU_Regs.reg_eax.word(0x0003);
            Callback.CALLBACK_RunRealInt(0x10);
        }
    };

    static private class copysource {
        String filename="";
        boolean concat=false;
        copysource(String filein,boolean concatin) {
            filename=filein;
            concat = concatin;
        }
        copysource() {

        }
    }

    handler CMD_COPY = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "COPY")) return;
            final String defaulttarget= ".";

            args.value = StripSpaces(args.value);
            /* Command uses dta so set it to our internal dta */
            /*RealPt*/int save_dta=Dos.dos.dta();
            Dos.dos.dta((int)Dos.dos.tables.tempdta);
            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());
            /*Bit32u*/LongRef size = new LongRef(0);/*Bit16u*/IntRef date=new IntRef(0);/*Bit16u*/IntRef time=new IntRef(0);/*Bit8u*/ShortRef attr=new ShortRef();
            StringRef name = new StringRef();
            Vector sources = new Vector();
            // ignore /b and /t switches: always copy binary
            while(ScanCMDBool(args,"B")) ;
            while(ScanCMDBool(args,"T")) ; //Shouldn't this be A ?
            while(ScanCMDBool(args,"A")) ;
            ScanCMDBool(args,"Y");
            ScanCMDBool(args,"-Y");
            ScanCMDBool(args,"V");

            String rem=ScanCMDRemain(args);
            if (rem!=null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"),new Object[] {rem});
                Dos.dos.dta(save_dta);
                return;
            }
            // Gather all sources (extension to copy more then 1 file specified at commandline)
            // Concatating files go as follows: All parts except for the last bear the concat flag.
            // This construction allows them to be counted (only the non concat set)
            String source_p;
            while ( (source_p = StringHelper.StripWord(args))!=null && source_p.length()>0 ) {
                do {
                    int plus = source_p.indexOf('+');
                    String source_x;
                    // If StripWord() previously cut at a space before a plus then
			        // set concatenate flag on last source and remove leading plus
                    if (plus == 0 && sources.size()>0) {
                        copysource s = (copysource)sources.elementAt(sources.size()-1);
                        s.concat = true;
                        if (source_p.length()==1)
                            break;
                        source_p = source_p.substring(1);
                        plus = source_p.indexOf('+');
                    }
                    if (plus>=0) {
                        source_x = source_p.substring(0, plus);
                    } else {
                        source_x = source_p;
                    }
                    boolean has_drive_spec = false;
                    int source_x_len = source_x.length();
                    if (source_x_len>0) {
                        if (source_x.charAt(source_x_len-1)==':') has_drive_spec = true;
                    }
                    if (!has_drive_spec && !source_p.contains("*") && !source_p.contains("?")) { //doubt that fu*\*.* is valid
                        if (Dos_files.DOS_FindFirst(source_x,0xffff & ~Dos_system.DOS_ATTR_VOLUME)) {
                            dta.GetResult(name,size,date,time,attr);
                            if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY)!=0)
                                source_x+="\\*.*";
                        }
                    }
                    sources.add(new copysource(source_x,(plus>=0)?true:false));
                    if (plus>=0) {
                        source_p = source_p.substring(plus+1);
                    } else {
                        source_p = "";
                    }
                } while(source_p.length()>0);
            }
            // At least one source has to be there
            if (sources.size()==0 || ((copysource)sources.elementAt(0)).filename.length()==0) {
                WriteOut(Msg.get("SHELL_MISSING_PARAMETER"));
                Dos.dos.dta(save_dta);
                return;
            }

            copysource target=new copysource();
            // If more then one object exists and last target is not part of a
            // concat sequence then make it the target.
            if(sources.size()>1 && !((copysource)sources.elementAt(sources.size()-2)).concat){
                target = (copysource)sources.lastElement();
                sources.removeElementAt(sources.size()-1);
            }
            //If no target => default target with concat flag true to detect a+b+c
            if(target.filename.length() == 0) target = new copysource(defaulttarget,true);

            copysource oldsource = new copysource();
            copysource source = new copysource();
            /*Bit32u*/int count = 0;
            while(sources.size()>0) {
                /* Get next source item and keep track of old source for concat start end */
                oldsource = source;
                source = (copysource)sources.firstElement();
                sources.remove(0);

                //Skip first file if doing a+b+c. Set target to first file
                if(!oldsource.concat && source.concat && target.concat) {
                    target = source;
                    continue;
                }

                /* Make a full path in the args */
                StringRef pathSource = new StringRef();
                StringRef pathTarget = new StringRef();

                if (!Dos_files.DOS_Canonicalize(source.filename,pathSource)) {
                    WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                    Dos.dos.dta(save_dta);
                    return;
                }
                // cut search pattern
                int pos = pathSource.value.lastIndexOf('\\');
                if (pos>=0) pathSource.value = pathSource.value.substring(0, pos+1);

                if (!Dos_files.DOS_Canonicalize(target.filename,pathTarget)) {
                    WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                    Dos.dos.dta(save_dta);
                    return;
                }
                int temp = pathTarget.value.indexOf("*.*");
                if(temp>=0) pathTarget.value = pathTarget.value.substring(0, temp);//strip off *.* from target

                // add '\\' if target is a directoy
                if (pathTarget.value.charAt(pathTarget.value.length()-1)!='\\') {
                    if (Dos_files.DOS_FindFirst(pathTarget.value,0xffff & ~Dos_system.DOS_ATTR_VOLUME)) {
                        dta.GetResult(name,size,date,time,attr);
                        if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY)!=0)
                            pathTarget.value+="\\";
                    }
                }

                //Find first sourcefile
                boolean ret = Dos_files.DOS_FindFirst(source.filename,0xffff & ~Dos_system.DOS_ATTR_VOLUME);
                if (!ret) {
                    WriteOut(Msg.get("SHELL_CMD_FILE_NOT_FOUND"),new Object[] {source.filename});
                    Dos.dos.dta(save_dta);
                    return;
                }

                String nameTarget;
                String nameSource;

                while (ret) {
                    dta.GetResult(name,size,date,time,attr);

                    if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY)==0) {
                        nameSource=pathSource.value;
                        nameSource+=name.value;
                        /*Bit16u*/IntRef sourceHandle = new IntRef(0),targetHandle = new IntRef(0);
                        // Open Source
                        if (Dos_files.DOS_OpenFile(nameSource,0,sourceHandle)) {
                            // Create Target or open it if in concat mode
                            nameTarget=pathTarget.value;
                            if (nameTarget.charAt(nameTarget.length()-1)=='\\') nameTarget+=name.value;

                            //Don't create a newfile when in concat mode
                            if (oldsource.concat || Dos_files.DOS_CreateFile(nameTarget,0,targetHandle)) {
                                /*Bit32u*/LongRef dummy=new LongRef(0);
                                //In concat mode. Open the target and seek to the eof
                                if (!oldsource.concat || (Dos_files.DOS_OpenFile(nameTarget,Dos_files.OPEN_READWRITE,targetHandle) &&
                                                          Dos_files.DOS_SeekFile(targetHandle.value,dummy,Dos_files.DOS_SEEK_END))) {
                                    // Copy
                                    final /*Bit8u*/byte[] buffer=new byte[0x8000];
                                    boolean	failed = false;
                                    /*Bit16u*/IntRef toread = new IntRef(0x8000);
                                    do {
                                        failed |= Dos_files.DOS_ReadFile(sourceHandle.value,buffer,toread);
                                        failed |= Dos_files.DOS_WriteFile(targetHandle.value,buffer,toread);
                                    } while (toread.value==0x8000);
                                    failed |= Dos_files.DOS_CloseFile(sourceHandle.value);
                                    failed |= Dos_files.DOS_CloseFile(targetHandle.value);
                                    WriteOut(" "+name.value+"\n");
                                    if(!source.concat) count++; //Only count concat files once
                                } else {
                                    Dos_files.DOS_CloseFile(sourceHandle.value);
                                    WriteOut(Msg.get("SHELL_CMD_COPY_FAILURE"),new Object[] {target.filename});
                                }
                            } else {
                                Dos_files.DOS_CloseFile(sourceHandle.value);
                                if (targetHandle.value != 0)
                                    Dos_files.DOS_CloseFile(targetHandle.value);
                                WriteOut(Msg.get("SHELL_CMD_COPY_FAILURE"),new Object[] {target.filename});
                            }
                        } else WriteOut(Msg.get("SHELL_CMD_COPY_FAILURE"),new Object[] {source.filename});
                    }
                    //On the next file
                    ret = Dos_files.DOS_FindNext();
                }
            }

            WriteOut(Msg.get("SHELL_CMD_COPY_SUCCESS"),new Object[] {new Integer(count)});
            Dos.dos.dta(save_dta);
        }
    };

    handler CMD_DATE = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "DATE")) return;
            if(ScanCMDBool(args,"h")) {
                // synchronize date with host parameter
                Calendar calendar = Calendar.getInstance();

                CPU_Regs.reg_ecx.word(calendar.get(Calendar.YEAR));
                CPU_Regs.reg_edx.high(calendar.get(Calendar.MONTH) + 1);
                CPU_Regs.reg_edx.low(calendar.get(Calendar.DAY_OF_MONTH));

                CPU_Regs.reg_eax.high(0x2b); // set system date
                Callback.CALLBACK_RunRealInt(0x21);
                return;
            }
            // check if a date was passed in command line
            String[] parts = StringHelper.split(args.value.trim(), "-");
            if(parts.length == 3) {
                try {
                    int newmonth = Integer.parseInt(parts[0]);
                    int newday = Integer.parseInt(parts[1]);
                    int newyear = Integer.parseInt(parts[2]);
                    CPU_Regs.reg_ecx.word(newyear);
                    CPU_Regs.reg_edx.high(newmonth);
                    CPU_Regs.reg_edx.low(newday);

                    CPU_Regs.reg_eax.high(0x2b); // set system date
                    Callback.CALLBACK_RunRealInt(0x21);
                    if(CPU_Regs.reg_eax.low()==0xff) WriteOut(Msg.get("SHELL_CMD_DATE_ERROR"));
                    return;
                } catch (Exception e) {

                }
            }
            // display the current date
            CPU_Regs.reg_eax.high(0x2a); // get system date
            Callback.CALLBACK_RunRealInt(0x21);

            String datestring = Msg.get("SHELL_CMD_DATE_DAYS");
            String day = "";
            try {
                int length = Integer.parseInt(datestring.substring(0, 1));
                if (datestring.length()==length*7+1) {
                    day = datestring.substring(1+length*CPU_Regs.reg_eax.low());
                    day = day.substring(0, length);
                }
            } catch (Exception e) {

            }
            boolean dateonly = ScanCMDBool(args,"t");
            if(!dateonly) WriteOut(Msg.get("SHELL_CMD_DATE_NOW"));

            String formatstring = Msg.get("SHELL_CMD_DATE_FORMAT");
            if (formatstring.length()!=5) return;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < 5; i++) {
                if(i==1 || i==3) {
                    buffer.append(formatstring.charAt(i));
                } else {
                    if(formatstring.charAt(i)=='M') buffer.append(StringHelper.sprintf("%02d", new Object[] {new Integer(CPU_Regs.reg_edx.high())}));
                    if(formatstring.charAt(i)=='D') buffer.append(StringHelper.sprintf("%02d", new Object[] {new Integer(CPU_Regs.reg_edx.low())}));
                    if(formatstring.charAt(i)=='Y') buffer.append(StringHelper.sprintf("%02d", new Object[] {new Integer(CPU_Regs.reg_ecx.word())}));
                }
            }
            WriteOut(day + " " + buffer.toString() + "\n");
            if(!dateonly) WriteOut(Msg.get("SHELL_CMD_DATE_SETHLP"));
        }
    };

    handler CMD_DIR = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "DIR")) return;

            StringRef line=new StringRef();
            if(GetEnvStr("DIRCMD",line)){
                int idx = line.value.indexOf('=');
                if (idx>=0) {
                    args.value+=" "+line.value.substring(idx+1);
                }
            }

            boolean optW=ScanCMDBool(args,"W");
            ScanCMDBool(args,"S");
            boolean optP=ScanCMDBool(args,"P");
            if (ScanCMDBool(args,"WP") || ScanCMDBool(args,"PW")) {
                optW=optP=true;
            }
            boolean optB=ScanCMDBool(args,"B");
            boolean optAD=ScanCMDBool(args,"AD");
            String rem=ScanCMDRemain(args);
            if (rem!=null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"),new Object[] {rem});
                return;
            }
            /*Bit32u*/long byte_count,file_count,dir_count;
            /*Bitu*/int w_count=0;
            /*Bitu*/int p_count=0;
            /*Bitu*/int w_size = optW?5:1;
            byte_count=file_count=dir_count=0;

            args.value = args.value.trim();
            if (args.value.length() == 0) {
                args.value="*.*"; //no arguments.
            } else {
                if (args.value.endsWith("\\") || args.value.endsWith(":")) {
                    args.value+="*.*";
                }
            }
            args.value = ExpandDot(args);

            if (args.value.indexOf("*")<0 && args.value.indexOf("?")<0) {
                /*Bit16u*/IntRef attribute=new IntRef(0);
                if(Dos_files.DOS_GetFileAttr(args.value,attribute) && (attribute.value & Dos_system.DOS_ATTR_DIRECTORY)!=0 ) {
                    args.value+="\\*.*";	// if no wildcard and a directory, get its files
                }
            }
            if (args.value.indexOf('.')<0) {
                args.value+=".*";	// if no extension, get them all
            }

            /* Make a full path in the args */
            StringRef path = new StringRef();
            if (!Dos_files.DOS_Canonicalize(args.value,path)) {
                WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                return;
            }
            path.value = path.value.substring(0, path.value.lastIndexOf('\\')+1);
            if (!optB) WriteOut(Msg.get("SHELL_CMD_DIR_INTRO"),new Object[] {path.value});

            /* Command uses dta so set it to our internal dta */
            /*RealPt*/int save_dta=Dos.dos.dta();
            Dos.dos.dta((int)Dos.dos.tables.tempdta);
            Dos_DTA dta=new Dos_DTA(Dos.dos.dta());
            boolean ret=Dos_files.DOS_FindFirst(args.value,0xffff & ~Dos_system.DOS_ATTR_VOLUME);
            if (!ret) {
                if (!optB) WriteOut(Msg.get("SHELL_CMD_FILE_NOT_FOUND"),new Object[] {args.value});
                Dos.dos.dta(save_dta);
                return;
            }

            do {    /* File name and extension */
                StringRef name=new StringRef();/*Bit32u*/LongRef size=new LongRef(0);/*Bit16u*/IntRef date=new IntRef(0);/*Bit16u*/IntRef time=new IntRef(0);/*Bit8u*/ShortRef attr=new ShortRef(0);
                dta.GetResult(name,size,date,time,attr);

                /* Skip non-directories if option AD is present */
                if(optAD && (attr.value&Dos_system.DOS_ATTR_DIRECTORY)==0 ) continue;

                /* output the file */
                if (optB) {
                    // this overrides pretty much everything
                    if (!name.equals(".") && !name.equals("..")) {
                        WriteOut(name+"\n");
                    }
                } else {
                    String ext = "";
                    if (!optW && !name.value.startsWith(".")) {
                        int pos = name.value.lastIndexOf('.');
                        if (pos>=0) {
                            ext = name.value.substring(pos+1);
                            name.value = name.value.substring(0, pos);
                        }
                    }
                    /*Bit8u*/short day = (/*Bit8u*/short)(date.value & 0x001f);
                    /*Bit8u*/short month	= (/*Bit8u*/short)((date.value >> 5) & 0x000f);
                    /*Bit16u*/int year = (/*Bit16u*/int)((date.value >> 9) + 1980);
                    /*Bit8u*/short hour	= (/*Bit8u*/short)((time.value >> 5 ) >> 6);
                    /*Bit8u*/short minute = (/*Bit8u*/short)((time.value >> 5) & 0x003f);

                    if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY)!=0) {
                        if (optW) {
                            WriteOut("["+name.value+"]");
                            int namelen = name.value.length();
                            if (namelen <= 14) {
                                for (int i=14-namelen;i>0;i--) WriteOut(" ");
                            }
                        } else {
                            WriteOut("%-8s %-3s   %-16s %02d-%02d-%04d %2d:%02d\n",new Object[] {name.value,ext,"<DIR>",new Integer(day),new Integer(month),new Integer(year),new Integer(hour),new Integer(minute)});
                        }
                        dir_count++;
                    } else {
                        if (optW) {
                            WriteOut("%-16s",new Object[] {name.value});
                        } else {
                            String numformat = FormatNumber(size.value);
                            WriteOut("%-8s %-3s   %16s %02d-%02d-%04d %2d:%02d\n",new Object[] {name.value,ext,numformat,new Integer(day),new Integer(month),new Integer(year),new Integer(hour),new Integer(minute)});
                        }
                        file_count++;
                        byte_count+=size.value;
                    }
                    if (optW) {
                        w_count++;
                    }
                }
                if (optP && (++p_count%(22*w_size))==0) {
                    CMD_PAUSE.call("");
                }
            } while ( (ret=Dos_files.DOS_FindNext()) );
            if (optW) {
                if ((w_count%5)!=0)	WriteOut("\n");
            }
            if (!optB) {
                /* Show the summary of results */
                String numformat = FormatNumber(byte_count);
                WriteOut(Msg.get("SHELL_CMD_DIR_BYTES_USED"),new Object[] {new Long(file_count),numformat});
                /*Bit8u*/short drive=dta.GetSearchDrive();
                //TODO Free Space
                /*Bitu*/int free_space=1024*1024*100;
                if (Dos_files.Drives[drive]!=null) {
                    /*Bit16u*/IntRef bytes_sector=new IntRef(0);/*Bit8u*/ShortRef sectors_cluster=new ShortRef();/*Bit16u*/IntRef total_clusters=new IntRef(0);/*Bit16u*/IntRef free_clusters=new IntRef(0);
                    Dos_files.Drives[drive].AllocationInfo(bytes_sector,sectors_cluster,total_clusters,free_clusters);
                    free_space=bytes_sector.value*sectors_cluster.value*free_clusters.value;
                }
                numformat = FormatNumber(free_space);
                WriteOut(Msg.get("SHELL_CMD_DIR_BYTES_FREE"),new Object[] {new Long(dir_count),numformat});
            }
            Dos.dos.dta(save_dta);
        }
    };

    handler CMD_DELETE = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "DELETE")) return;
            /* Command uses dta so set it to our internal dta */
            /*RealPt*/int save_dta=Dos.dos.dta();
            Dos.dos.dta((int)Dos.dos.tables.tempdta);

            String rem=ScanCMDRemain(args);
            if (rem!=null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"),new Object[] {rem});
                return;
            }
            /* If delete accept switches mind the space infront of them. See the dir /p code */

            StringRef full = new StringRef();
            args.value = ExpandDot(args);
            args.value = StripSpaces(args.value);
            if (!Dos_files.DOS_Canonicalize(args.value,full)) { WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));return; }
        //TODO Maybe support confirmation for *.* like dos does.
            boolean res=Dos_files.DOS_FindFirst(args.value,0xffff & ~Dos_system.DOS_ATTR_VOLUME);
            if (!res) {
                WriteOut(Msg.get("SHELL_CMD_DEL_ERROR"),new Object[] {args});
                Dos.dos.dta(save_dta);
                return;
            }
            String path = full.value.substring(0, full.value.lastIndexOf("\\"));
            StringRef name=new StringRef();/*Bit32u*/LongRef size=new LongRef(0);/*Bit16u*/IntRef time=new IntRef(0),date=new IntRef(0);/*Bit8u*/ShortRef attr=new ShortRef(0);
            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());
            while (res) {
                dta.GetResult(name,size,date,time,attr);
                if ((attr.value & (Dos_system.DOS_ATTR_DIRECTORY|Dos_system.DOS_ATTR_READ_ONLY))==0) {
                    if (!Dos_files.DOS_UnlinkFile(path+name.value)) WriteOut(Msg.get("SHELL_CMD_DEL_ERROR"),new Object[] {full.value});
                }
                res=Dos_files.DOS_FindNext();
            }
            Dos.dos.dta(save_dta);
        }
    };

    handler CMD_ECHO = new handler() {
        public void call(String args) {
            if (args.length()==0) {
                if (echo) { WriteOut(Msg.get("SHELL_CMD_ECHO_ON"));}
                else { WriteOut(Msg.get("SHELL_CMD_ECHO_OFF"));}
                return;
            }
            String cmd=StripSpaces(args);
            if (cmd.equalsIgnoreCase("OFF")) {
                echo=false;
                return;
            }
            if (cmd.equalsIgnoreCase("ON")) {
                echo=true;
                return;
            }
            StringRef a = new StringRef(cmd);
            if (cmd.equalsIgnoreCase("/?")) { if (HELP(a, "ECHO")) return; }

            args=args.substring(1);//skip first character. either a slash or dot or space
            //TODO check input of else ook nodig is.
            if(args.endsWith("\r")) {
                Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"Hu ? carriage return already present. Is this possible?");
                WriteOut(args+"\n");
            } else WriteOut(args+"\r\n");
        }
    };
    handler CMD_EXIT = new handler() {
        public void call(String args) {
            StringRef a = new StringRef(args);
            if (HELP(a, "EXIT")) return;
            exit = true;
        }
    };
    handler CMD_MKDIR = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "MKDIR")) return;
            args.value = StripSpaces(args.value);
            String rem=ScanCMDRemain(args);
            if (rem!=null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"),new Object[] {rem});
                return;
            }
            if (!Dos_files.DOS_MakeDir(args.value)) {
                WriteOut(Msg.get("SHELL_CMD_MKDIR_ERROR"),new Object[] {args});
            }
        }
    };

    handler CMD_CHDIR = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CHDIR")) return;
            args.value = StripSpaces(args.value);
            if (args.value.length()==0) {
                /*Bit8u*/char drive=(char)(Dos_files.DOS_GetDefaultDrive()+'A');
                StringRef dir = new StringRef();
                Dos_files.DOS_GetCurrentDir((short)0,dir);
                WriteOut(String.valueOf(drive)+":\\"+dir+"\n");
            } else if(args.value.length() == 2 && args.value.charAt(1)==':') {
                WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT"),new Object[]{args.value.toUpperCase()});
            } else 	if (!Dos_files.DOS_ChangeDir(args.value)) {
                /* Changedir failed. Check if the filename is longer then 8 and/or contains spaces */

                String temps=args.value.toUpperCase();
                temps = StringHelper.replace(temps, "/", "\\");
                String[] slash = StringHelper.split(temps, "\\");
                StringBuffer shortversion = new StringBuffer();

                boolean space = false;
                boolean toolong = false;

                for (int i=0;i<slash.length;i++) {
                    if (slash[i].indexOf(' ')>=0) space = true;
                    if (slash[i].length()>8) toolong = true;
                    String s = slash[i];
                    s = StringHelper.replace(s, " ", "");
                    s = StringHelper.replace(s, ".", "");
                    s = StringHelper.replace(s, "\"", "");
                    if (s.length()>6)
                        s = s.substring(0, 6)+"~1";
                    if (i>0)
                        shortversion.append("\\");
                    shortversion.append(s);
                }
                if (space) {/* Contains spaces */
                    WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT_2"),new Object[]{shortversion.toString()});
                } else if (toolong) {
                    WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT_2"),new Object[]{shortversion.toString()});
                } else {
                    /*Bit8u*/char drive=(char)(Dos_files.DOS_GetDefaultDrive()+'A');
                    if (drive=='Z') {
                        WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT_3"));
                    } else {
                        WriteOut(Msg.get("SHELL_CMD_CHDIR_ERROR"),new Object[]{args.value});
                    }
                }
            }
        }
    };

    handler CMD_RMDIR = new handler() {
        public void call(String ar) {
            StringRef args = new StringRef(ar);
            if (HELP(args, "RMDIR")) return;
            args.value = StripSpaces(args.value);
            String rem=ScanCMDRemain(args);
            if (rem!=null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"),new Object[]{rem});
                return;
            }
            if (!Dos_files.DOS_RemoveDir(args.value)) {
                WriteOut(Msg.get("SHELL_CMD_RMDIR_ERROR"),new Object[]{args});
            }
        }
    };

    handler CMD_SET = new handler() {
        public void call(String ar) {
            StringRef args = new StringRef(ar);
            if (HELP(args, "SET")) return;
            args.value = StripSpaces(args.value);
            StringRef line = new StringRef();
            if (args.value.length() == 0) {
                /* No command line show all environment lines */
                /*Bitu*/int count=GetEnvCount();
                for (/*Bitu*/int a=0;a<count;a++) {
                    if (GetEnvNum(a,line)) WriteOut(line.value+"\n");
                }
                return;
            }
            int p=args.value.indexOf("=");
            if (p<0) {
                if (!GetEnvStr(args.value,line)) WriteOut(Msg.get("SHELL_CMD_SET_NOT_SET"),new Object[]{args.value});
                WriteOut(line.value+"\n");
            } else {
                String key = args.value.substring(0, p);
                p++;
                /* parse p for envirionment variables */
                StringBuffer parsed = new StringBuffer();
                while (p<args.value.length()) {
                    char c = args.value.charAt(p);
                    if(c != '%') {parsed.append(c);p++;} //Just add it (most likely path)
                    else if(p+1<args.value.length() && args.value.charAt(p+1) == '%') {
                        parsed.append('%'); p += 2; //%% => %
                    } else {
                        int second = args.value.indexOf('%', p+1);
                        if(second<0) continue;
                        StringRef temp=new StringRef();
                        if (GetEnvStr(args.value.substring(p+1, second),temp)) {
                            int pos = temp.value.indexOf('=');
                            if (pos<0) continue;
                            parsed.append(temp.value.substring(pos+1));
                        }
                        p = second+1;
                    }
                }
                /* Try setting the variable */
                if (!SetEnv(key,parsed.toString())) {
                    WriteOut(Msg.get("SHELL_CMD_SET_OUT_OF_SPACE"));
                }
            }
        }
    };

    handler CMD_IF = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "IF")) return;
            args.value = StripSpaces(args.value,'=');
            boolean has_not=false;

            while (args.value.toUpperCase().startsWith("NOT") && args.value.length()>3) {
                if (!StringHelper.isspace(args.value.charAt(3)) && args.value.charAt(3) != '=') break;
                args.value = args.value.substring(3);	//skip text
                //skip more spaces
                args.value=StripSpaces(args.value,'=');
                has_not = !has_not;
            }

            if(args.value.toUpperCase().startsWith("ERRORLEVEL")) {
                args.value = args.value.substring(10);	//skip text
                //Strip spaces and ==
                args.value = StripSpaces(args.value,'=');
                String word = StringHelper.StripWord(args);
                if(!StringHelper.isdigit(word.charAt(0))) {
                    WriteOut(Msg.get("SHELL_CMD_IF_ERRORLEVEL_MISSING_NUMBER"));
                    return;
                }

                /*Bit8u*/int n = 0;
                do {
                    n = n * 10 + (word.charAt(0) - '0');
                    word = word.substring(1);
                } while (word.length()>0 && StringHelper.isdigit(word.charAt(0)));
                if(word.length()>0 && !StringHelper.isspace(word.charAt(0))) {
                    WriteOut(Msg.get("SHELL_CMD_IF_ERRORLEVEL_INVALID_NUMBER"));
                    return;
                }
                /* Read the error code from DOS */
                if ((Dos.dos.return_code>=n) ==(!has_not)) DoCommand(args.value);
                return;
            }

            if(args.value.toUpperCase().startsWith("EXIST ")) {
                args.value = args.value.substring(6);	//skip text
                args.value = StripSpaces(args.value);
                String word = StringHelper.StripWord(args);
                if (word.length()==0) {
                    WriteOut(Msg.get("SHELL_CMD_IF_EXIST_MISSING_FILENAME"));
                    return;
                }

                {	/* DOS_FindFirst uses dta so set it to our internal dta */
                    /*RealPt*/int save_dta=Dos.dos.dta();
                    Dos.dos.dta((int)Dos.dos.tables.tempdta);
                    boolean ret=Dos_files.DOS_FindFirst(word,0xffff & ~Dos_system.DOS_ATTR_VOLUME);
                    Dos.dos.dta(save_dta);
                    if (ret==(!has_not)) DoCommand(args.value);
                }
                return;
            }

            /* Normal if string compare */

            String word = "";
            // first word is until space or =
            while (args.value.length()>0 && !StringHelper.isspace(args.value.charAt(0)) && args.value.charAt(0) != '=') {
                word+=args.value.substring(0, 1);
                args.value = args.value.substring(1);
            }

            // scan for =
            while (args.value.length()>0 && args.value.charAt(0) != '=') {
                args.value = args.value.substring(1);
            }
            // check for ==
            if (args.value.length()<2 || args.value.charAt(1) != '=') {
                SyntaxError();
                return;
            }
            args.value = args.value.substring(2);
            args.value = StripSpaces(args.value,'=');

            String word2 = "";
            // second word is until space or =
            while (args.value.length()>0 && !StringHelper.isspace(args.value.charAt(0)) && args.value.charAt(0) != '=') {
                word2+=args.value.substring(0, 1);
                args.value = args.value.substring(1);
            }

            if (args.value.length()>0) {
                args.value = StripSpaces(args.value,'=');

                if (word.equals(word2)==(!has_not)) DoCommand(args.value);
            }
        }
    };

    handler CMD_GOTO = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "GOTO")) return;
            args.value=StripSpaces(args.value);
            if (bf==null) return;
            if (args.value.length()>0  && (args.value.charAt(0)==':')) args.value = args.value.substring(1);
            //label ends at the first space
            for (int i=0;i<args.value.length();i++) {
                if (args.value.charAt(0)==' ' || args.value.charAt(0)=='\t') {
                    args.value = args.value.substring(0, i);
                    break;
                }
            }
            if (args.value.length()==0) {
                WriteOut(Msg.get("SHELL_CMD_GOTO_MISSING_LABEL"));
                return;
            }
            if (!bf.Goto(args.value)) {
                WriteOut(Msg.get("SHELL_CMD_GOTO_LABEL_NOT_FOUND"),new Object[]{args});
                return;
            }
        }
    };

    handler CMD_TIME = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "TIME")) return;
            if(ScanCMDBool(args,"h")) {
                // synchronize date with host parameter
                Calendar calendar = Calendar.getInstance();

                // reg_ah=0x2d; // set system time TODO
                // CALLBACK_RunRealInt(0x21);
                long ticks=(long)(((double)(calendar.get(Calendar.HOUR_OF_DAY)*3600+
                                                calendar.get(Calendar.MINUTE)*60+
                                                calendar.get(Calendar.SECOND)))*18.206481481);
                Memory.mem_writed(Bios.BIOS_TIMER, (int)ticks);
                return;
            }
            boolean timeonly = ScanCMDBool(args,"t");

            CPU_Regs.reg_eax.high(0x2c); // get system time
            Callback.CALLBACK_RunRealInt(0x21);
        /*
                reg_dl= // 1/100 seconds
                reg_dh= // seconds
                reg_cl= // minutes
                reg_ch= // hours
        */
            if(timeonly) {
                WriteOut(StringHelper.sprintf("%2d:%02d\n", new Object[] {new Integer(CPU_Regs.reg_ecx.high()), new Integer(CPU_Regs.reg_ecx.low())}));
            } else {
                WriteOut(Msg.get("SHELL_CMD_TIME_NOW"));
                WriteOut(StringHelper.sprintf("%2d:%02d:%02d,%02d\n", new Object[] {new Integer(CPU_Regs.reg_ecx.high()), new Integer(CPU_Regs.reg_ecx.low()), new Integer(CPU_Regs.reg_edx.high()), new Integer(CPU_Regs.reg_edx.low())}));
            }
        }
    };

    handler CMD_TYPE = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "TYPE")) return;
            args.value = StripSpaces(args.value);
            if (args.value.length()==0) {
                WriteOut(Msg.get("SHELL_SYNTAXERROR"));
                return;
            }
            /*Bit16u*/IntRef handle=new IntRef(0);
            while (true) {
                String word=StringHelper.StripWord(args);
                if (!Dos_files.DOS_OpenFile(word,0,handle)) {
                    WriteOut(Msg.get("SHELL_CMD_FILE_NOT_FOUND"),new Object[]{word});
                    return;
                }
                /*Bit16u*/IntRef n=new IntRef(0);/*Bit8u*/byte[] c=new byte[1];
                do {
                    n.value=1;
                    Dos_files.DOS_ReadFile(handle.value,c,n);
                    Dos_files.DOS_WriteFile(Dos_files.STDOUT,c,n);
                } while (n.value>0);
                Dos_files.DOS_CloseFile(handle.value);
                if (args.value.length()==0) break;
            }
        }
    };

    handler CMD_REM = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            HELP(args, "REM");
        }
    };

    handler CMD_RENAME = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "RENAME")) return;
            args.value = StripSpaces(args.value);
            if(args.value.length()==0) {SyntaxError();return;}
            if(args.value.indexOf('*')>=0 || args.value.indexOf('?')>=0 ) { WriteOut(Msg.get("SHELL_CMD_NO_WILD"));return;}
            String arg1=StringHelper.StripWord(args);
            int slash = arg1.lastIndexOf('\\');
            if(slash>=0) {
                /* If directory specified (crystal caves installer)
                 * rename from c:\X : rename c:\abc.exe abc.shr.
                 * File must appear in C:\ */

                String dir_source = arg1.substring(0, slash);
                slash++;

                if(dir_source.length() == 2 && dir_source.charAt(1) == ':')
                    dir_source+="\\"; //X: add slash

                StringRef dir_current = new StringRef();
                Dos_files.DOS_GetCurrentDir((short)0,dir_current);
                if(!Dos_files.DOS_ChangeDir(dir_source)) {
                    WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                    return;
                }
                Dos_files.DOS_Rename(arg1.substring(slash),args.value);
                Dos_files.DOS_ChangeDir(dir_current.value);
            } else {
                Dos_files.DOS_Rename(arg1,args.value);
            }
        }
    };

    handler CMD_CALL = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CALL")) return;
            call=true; /* else the old batchfile will be closed first */
            ParseLine(args.value);
            call=false;
        }
    };
    void SyntaxError() {
        WriteOut(Msg.get("SHELL_SYNTAXERROR"));
    }
    
    handler CMD_PAUSE = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "PAUSE")) return;
            WriteOut(Msg.get("SHELL_CMD_PAUSE"));
            /*Bit8u*/byte[] c=new byte[1];/*Bit16u*/IntRef n=new IntRef(1);
            Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
        }
    };

    handler CMD_SUBST = new handler() {
        public void call(String a) {
            /* If more that one type can be substed think of something else
             * E.g. make basedir member dos_drive instead of localdrive
             */
            StringRef args = new StringRef(a);
            if (HELP(args, "SUBST")) return;
            String mountstring;

            mountstring = "MOUNT ";
            args.value = StripSpaces(args.value);
            String arg;
            CommandLine command = new CommandLine(null,args.value);

            if (command.GetCount() != 2) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }

            arg = command.FindCommand(1);
            if( (arg.length()>1) && arg.charAt(1) !=':')  {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            arg = command.FindCommand(2);
            String temp_str=args.value.substring(0,1).toUpperCase();
            if (arg.toUpperCase().equals("/D" )) {
                if(Dos_files.Drives[temp_str.charAt(0)-'A']==null ) {
                    WriteOut(Msg.get("SHELL_CMD_SUBST_NO_REMOVE"));
                    return;
                }
                mountstring+="-u ";
                mountstring+=temp_str;
                ParseLine(mountstring);
                return;
            }
            if(Dos_files.Drives[temp_str.charAt(0)-'A']!=null ) {
                //targetdrive in use
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            mountstring+=temp_str;
            mountstring+=" ";

            /*Bit8u*/ShortRef drive=new ShortRef(0);StringRef fulldir = new StringRef();
            if (!Dos_files.DOS_MakeName(arg,fulldir,drive)) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }

            if(!(Dos_files.Drives[drive.value] instanceof Drive_local)) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            Drive_local ldp=(Drive_local)Dos_files.Drives[drive.value];
            StringRef newname=new StringRef(ldp.basedir);
            newname.value+=fulldir.value;
            //CROSS_FILENAME(newname);
            ldp.dirCache.ExpandName(newname);
            mountstring+="\"";
            mountstring+=newname.value;
            mountstring+="\"";
            ParseLine(mountstring);
        }
    };

    handler CMD_LOADHIGH = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "LOADHIGH")) return;
            /*Bit16u*/int umb_start=Dos.dos_infoblock.GetStartOfUMBChain();
            /*Bit8u*/short umb_flag=Dos.dos_infoblock.GetUMBChainState();
            /*Bit8u*/short old_memstrat=(/*Bit8u*/short)(Dos_memory.DOS_GetMemAllocStrategy()&0xff);
            if (umb_start==0x9fff) {
                if ((umb_flag&1)==0) Dos_memory.DOS_LinkUMBsToMemChain(1);
                Dos_memory.DOS_SetMemAllocStrategy(0x80);	// search in UMBs first
                ParseLine(args.value);
                /*Bit8u*/short current_umb_flag=Dos.dos_infoblock.GetUMBChainState();
                if ((current_umb_flag&1)!=(umb_flag&1)) Dos_memory.DOS_LinkUMBsToMemChain(umb_flag);
                Dos_memory.DOS_SetMemAllocStrategy(old_memstrat);	// restore strategy
            } else ParseLine(args.value);
        }
    };

    static private class DefaultChoice extends Thread {
        int timeout = 0;
        byte[] choice;
        Object mutex = new Object();

        public void run() {
            synchronized(mutex) {
                try {
                    mutex.wait(timeout*1000);
                    Bios_keyboard.BIOS_AddKeyToBuffer(choice[0]);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    handler CMD_CHOICE = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CHOICE")) return;
            final String defchoice = "yn";
            String rem = null;
            boolean optN = ScanCMDBool(args,"N");
            boolean optS = ScanCMDBool(args,"S"); //Case-sensitive matching
            boolean timeout = false;
            String timeoutChoice="";
            int timeoutTime = -1;

            if (args.value.indexOf("/T")>=0) {
                int pos1 = args.value.indexOf("/T");
                int pos2 = args.value.indexOf(" ", pos1);
                String command = args.value.substring(pos1+2, pos2);
                args.value = args.value.substring(0, pos1)+args.value.substring(pos2+1);
                if (command.startsWith(":")) {
                    command = command.substring(1);
                }
                int pos3=command.indexOf(",");
                if (pos3>=0) {
                    timeoutChoice = command.substring(0,pos3);
                    try {
                        timeoutTime = Integer.parseInt(command.substring(pos3+1));
                        timeout = true;
                    } catch (Exception e) {
                    }
                }
            }
            //ScanCMDBool(args,"T"); //Default Choice after timeout
            if (args.value.length()>0) {
                args.value = StripSpaces(args.value);
                rem = ScanCMDRemain(args);
                if (rem!=null && rem.toLowerCase().charAt(0) != 'c') {
                    WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"),new Object[]{rem});
                    return;
                }
                if (rem != null && args.value.substring(1).startsWith(rem)) args.value = args.value.substring(rem.length()+1);
                if (rem != null) rem = rem.substring(2);
                if (rem != null && rem.charAt(0)==':') rem = rem.substring(1); /* optional : after /c */
            }
            if (rem==null || rem.length()==0) rem = defchoice; /* No choices specified use YN */
            if(!optS) rem = rem.toUpperCase(); /* When in no case-sensitive mode. make everything upcase */
            if(args.value.length()>0) {
                args.value = StripSpaces(args.value);
                int argslen = args.value.length();
                if(argslen>1 && args.value.charAt(0) == '"' && args.value.charAt(argslen-1) =='"') {
                    args.value = args.value.substring(1, argslen-1);
                }
                WriteOut(args.value);
            }
            /* Show question prompt of the form [a,b]? where a b are the choice values */
            if (!optN) {
                if(args.value.length()>0) WriteOut(" ");
                WriteOut("[");
                int len = rem.length();
                for(int t = 1; t < len; t++) {
                    WriteOut(String.valueOf(rem.charAt(t-1))+",");
                }
                WriteOut(String.valueOf(rem.charAt(len-1))+"]?");
            }

            /*Bit16u*/IntRef n=new IntRef(1);
            byte[] c = new byte[1];
            int pos;
            do {
                DefaultChoice defaultChoice = null;
                if (timeout) {
                    defaultChoice = new DefaultChoice();
                    defaultChoice.choice = timeoutChoice.getBytes();
                    defaultChoice.timeout = timeoutTime;
                    defaultChoice.start();
                }
                Dos_files.DOS_ReadFile(Dos_files.STDIN,c,n);
                if (defaultChoice != null) {
                    defaultChoice.interrupt();
                    try {defaultChoice.join(1000);} catch (Exception e) {}
                }
                if (optS)
                    pos = rem.indexOf((char)c[0]);
                else
                    pos = rem.indexOf(new String(c).toUpperCase());
            } while (pos<0);
            c = optS?c:new String(c).toUpperCase().getBytes();
            Dos_files.DOS_WriteFile(Dos_files.STDOUT,c, n);
            Dos.dos.return_code = (short)(pos+1);
        }
    };

    handler CMD_ATTRIB = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "ATTRIB")) return;
    	    // No-Op for now.
        }
    };
    handler CMD_PATH = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "PATH")) return;
            if(args.value.length()>0){
                String pathstring="set PATH=";
                while(args.value.length()>0 && (args.value.charAt(0)=='='|| args.value.charAt(0)==' '))
                     args.value=args.value.substring(1);
                pathstring+=args.value;
                ParseLine(pathstring);
                return;
            } else {
                StringRef line = new StringRef();
                if(GetEnvStr("PATH",line)) {
                    WriteOut(line.value);
                } else {
                    WriteOut("PATH=(null)");
                }
            }
        }
    };

    handler CMD_SHIFT = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "SHIFT")) return;
            if(bf!=null) bf.Shift();
        }
    };

    handler CMD_VER = new handler() {
        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "VER")) return;
            if (args.value.length()>0) {
                String word = StringHelper.StripWord(args);
                if(!word.equalsIgnoreCase("set")) return;
                word = StringHelper.StripWord(args);
                try {
                    Dos.dos.version.major = (byte)Integer.parseInt(word);
                    Dos.dos.version.minor = (byte)Integer.parseInt(args.value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else WriteOut(Msg.get("SHELL_CMD_VER_VER"),new Object[]{Config.VERSION,new Integer(Dos.dos.version.major),new Integer(Dos.dos.version.minor)});
        }
    };

    SHELL_Cmd[] cmd_list={
        new SHELL_Cmd(	"DIR",		0,			CMD_DIR,		"SHELL_CMD_DIR_HELP"),
        new SHELL_Cmd(	"CHDIR",	1,			CMD_CHDIR,		"SHELL_CMD_CHDIR_HELP"),
        new SHELL_Cmd(	"ATTRIB",	1,			CMD_ATTRIB,		"SHELL_CMD_ATTRIB_HELP"),
        new SHELL_Cmd(	"CALL",		1,			CMD_CALL,		"SHELL_CMD_CALL_HELP"),
        new SHELL_Cmd(	"CD",		0,			CMD_CHDIR,		"SHELL_CMD_CHDIR_HELP"),
        new SHELL_Cmd(	"CHOICE",	1,			CMD_CHOICE,		"SHELL_CMD_CHOICE_HELP"),
        new SHELL_Cmd(	"CLS",		0,			CMD_CLS,		"SHELL_CMD_CLS_HELP"),
        new SHELL_Cmd(	"COPY",		0,			CMD_COPY,		"SHELL_CMD_COPY_HELP"),
        new SHELL_Cmd(	"DATE",		0,			CMD_DATE,		"SHELL_CMD_DATE_HELP"),
        new SHELL_Cmd(	"DEL",		0,			CMD_DELETE,		"SHELL_CMD_DELETE_HELP"),
        new SHELL_Cmd(	"DELETE",	1,			CMD_DELETE,		"SHELL_CMD_DELETE_HELP"),
        new SHELL_Cmd(	"ERASE",	1,			CMD_DELETE,		"SHELL_CMD_DELETE_HELP"),
        new SHELL_Cmd(	"ECHO",		1,			CMD_ECHO,		"SHELL_CMD_ECHO_HELP"),
        new SHELL_Cmd(	"EXIT",		0,			CMD_EXIT,		"SHELL_CMD_EXIT_HELP"),
        new SHELL_Cmd(	"GOTO",		1,			CMD_GOTO,		"SHELL_CMD_GOTO_HELP"),
        new SHELL_Cmd(	"HELP",		1,			CMD_HELP,		"SHELL_CMD_HELP_HELP"),
        new SHELL_Cmd(	"IF",		1,			CMD_IF,			"SHELL_CMD_IF_HELP"),
        new SHELL_Cmd(	"LOADHIGH",	1,			CMD_LOADHIGH, 	"SHELL_CMD_LOADHIGH_HELP"),
        new SHELL_Cmd(	"LH",		1,			CMD_LOADHIGH,	"SHELL_CMD_LOADHIGH_HELP"),
        new SHELL_Cmd(	"MKDIR",	1,			CMD_MKDIR,		"SHELL_CMD_MKDIR_HELP"),
        new SHELL_Cmd(	"MD",		0,			CMD_MKDIR,		"SHELL_CMD_MKDIR_HELP"),
        new SHELL_Cmd(	"PATH",		1,			CMD_PATH,		"SHELL_CMD_PATH_HELP"),
        new SHELL_Cmd(	"PAUSE",	1,			CMD_PAUSE,		"SHELL_CMD_PAUSE_HELP"),
        new SHELL_Cmd(	"RMDIR",	1,			CMD_RMDIR,		"SHELL_CMD_RMDIR_HELP"),
        new SHELL_Cmd(	"RD",		0,			CMD_RMDIR,		"SHELL_CMD_RMDIR_HELP"),
        new SHELL_Cmd(	"REM",		1,			CMD_REM,		"SHELL_CMD_REM_HELP"),
        new SHELL_Cmd(	"RENAME",	1,			CMD_RENAME,		"SHELL_CMD_RENAME_HELP"),
        new SHELL_Cmd(	"REN",		0,			CMD_RENAME,		"SHELL_CMD_RENAME_HELP"),
        new SHELL_Cmd(	"SET",		1,			CMD_SET,		"SHELL_CMD_SET_HELP"),
        new SHELL_Cmd(	"SHIFT",	1,			CMD_SHIFT,		"SHELL_CMD_SHIFT_HELP"),
        new SHELL_Cmd(	"SUBST",	1,			CMD_SUBST,		"SHELL_CMD_SUBST_HELP"),
        new SHELL_Cmd(	"TIME",		0,			CMD_TIME,		"SHELL_CMD_TIME_HELP"),
        new SHELL_Cmd(	"TYPE",		0,			CMD_TYPE,		"SHELL_CMD_TYPE_HELP"),
        new SHELL_Cmd(	"VER",		0,			CMD_VER,		"SHELL_CMD_VER_HELP"),
        new SHELL_Cmd(null,0,null,null)
        };

}
