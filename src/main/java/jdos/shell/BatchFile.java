package jdos.shell;

import jdos.dos.Dos_files;
import jdos.misc.Log;
import jdos.misc.setup.CommandLine;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringHelper;
import jdos.util.StringRef;

public class BatchFile {
    BatchFile(Dos_shell host,String resolved_name,String entered_name, String cmd_line) {
        location.value = 0;
        prev=host.bf;
        echo=host.echo;
        shell=host;
        StringRef totalname=new StringRef();
        Dos_files.DOS_Canonicalize(resolved_name,totalname); // Get fullname including drive specificiation
        cmd = new CommandLine(entered_name,cmd_line);
        filename = totalname.value;

        //Test if file is openable
        if (!Dos_files.DOS_OpenFile(totalname.value,128, file_handle)) {
            //TODO Come up with something better
            Log.exit("SHELL:Can't open BatchFile "+totalname.value);
        }
        Dos_files.DOS_CloseFile(file_handle.value);
    }

    void close() {
        shell.bf=prev;
        shell.echo=echo;
    }
    
    String ReadLine() {
        //Open the batchfile and seek to stored postion
        if (!Dos_files.DOS_OpenFile(filename,128,file_handle)) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR,"ReadLine Can't open BatchFile "+filename);
            close();
            return null;
        }
        Dos_files.DOS_SeekFile(file_handle.value, location, Dos_files.DOS_SEEK_SET);

        /*Bit8u*/byte[] c=new byte[1];IntRef n=new IntRef(1);
        StringBuffer l;
        do {
            l=new StringBuffer();
            do {
                n.value=1;
                Dos_files.DOS_ReadFile(file_handle.value,c,n);
                if (n.value>0) {
                    /* Why are we filtering this ?
                     * Exclusion list: tab for batch files
                     * escape for ansi
                     * backspace for alien odyssey */
                    if (c[0]>31 || c[0]==0x1b || c[0]=='\t' || c[0]==8)
                        l.append((char)c[0]);
                }
            } while (c[0]!='\n' && n.value!=0);
            if (n.value==0 && l.length()==0) {
                //Close file and delete bat file
                Dos_files.DOS_CloseFile(file_handle.value);
                close();
                return null;
            }
        } while (l.length()==0 || l.charAt(0)==':');
        String in = l.toString();
        StringBuffer out = new StringBuffer();
        /* Now parse the line read from the bat file for % stuff */
        while (in.length()>0) {
            if (in.charAt(0)=='%') {
                in = in.substring(1);if (in.length()==0) break;
                if (in.charAt(0) == '%') {
                    in = in.substring(1);
                    out.append('%');
                    continue;
                }
                if (in.charAt(0) == '0') {  /* Handle %0 */
                    in = in.substring(1);
                    out.append(cmd.GetFileName());
                    continue;
                }
                char next = in.charAt(0);
                if(next > '0' && next <= '9') {
                    /* Handle %1 %2 .. %9 */
                    in = in.substring(1); //Progress reader
                    next -= '0';
                    if (cmd.GetCount()<next) continue;
                    String word;
                    if ((word=cmd.FindCommand((int)next))==null) continue;
                    out.append(word);
                    continue;
                } else {
                    /* Not a command line number has to be an environment */
                    int pos = in.indexOf('%');
                    /* No env afterall.Somewhat of a hack though as %% and % aren't handled consistent in dosbox. Maybe echo needs to parse % and %% as well. */
                    if (pos<0) {out.append("%");continue;}
                    StringRef env = new StringRef();
                    if (shell.GetEnvStr(in.substring(0,pos),env)) {
                        int pos2 = env.value.indexOf('=');
                        if (pos2<0) continue;
                        out.append(env.value.substring(pos2+1));
                    }
                    in = in.substring(pos+1);
                }
            } else {
                out.append(in.charAt(0));
                in = in.substring(1);
            }
        }
        //Store current location and close bat file
        this.location.value = 0;
        Dos_files.DOS_SeekFile(file_handle.value,location,Dos_files.DOS_SEEK_CUR);
        Dos_files.DOS_CloseFile(file_handle.value);
        return out.toString();
    }

    boolean Goto(String where) {
        //Open bat file and search for the where string
        if (!Dos_files.DOS_OpenFile(filename,128,file_handle)) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR,"SHELL:Goto Can't open BatchFile "+filename);
            close();
            return false;
        }

        /* Scan till we have a match or return false */
        /*Bit8u*/byte[] c=new byte[1];IntRef n=new IntRef(1);
        //again:
        while (true) {
            StringBuffer l=new StringBuffer();
            do {
                n.value=1;
                Dos_files.DOS_ReadFile(file_handle.value,c,n);
                if (n.value>0) {
                    /* Why are we filtering this ?
                     * Exclusion list: tab for batch files
                     * escape for ansi
                     * backspace for alien odyssey */
                    if (c[0]>31)
                        l.append((char)c[0]);
                }
            } while (c[0]!='\n' && n.value!=0);

            String nospace = l.toString().trim();
            if (nospace.length()>0 && nospace.charAt(0) == ':') {
                nospace=nospace.substring(1); //Skip :
                //Strip spaces and = from it.
                while (nospace.length()>0 && StringHelper.isspace(nospace.charAt(0)) || nospace.charAt(0)=='=') {
                    nospace = nospace.substring(1);
                }
                String beginLabel = nospace;
                while (nospace.length()>0 && !StringHelper.isspace(nospace.charAt(0)) && nospace.charAt(0)!='=') {
                    nospace = nospace.substring(1);
                }
                if (where.equalsIgnoreCase(beginLabel.substring(0, beginLabel.length()-nospace.length()))) {
                    //Found it! Store location and continue
                    this.location.value = 0;
                    Dos_files.DOS_SeekFile(file_handle.value,location,Dos_files.DOS_SEEK_CUR);
                    Dos_files.DOS_CloseFile(file_handle.value);
                    return true;
                }

            }
            if (n.value==0) {
                Dos_files.DOS_CloseFile(file_handle.value);
                close();
                return false;
            }
        }
    }

    void Shift() {
        cmd.Shift(1);
    }
    /*Bit16u*/IntRef file_handle=new IntRef(0);
    /*Bit32u*/ LongRef location=new LongRef(0);
    boolean echo;
    Dos_shell shell;
    BatchFile prev;
    CommandLine cmd;
    public String filename;
}
