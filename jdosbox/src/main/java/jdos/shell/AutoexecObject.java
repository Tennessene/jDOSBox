package jdos.shell;

import jdos.dos.drives.Drive_virtual;
import jdos.misc.Log;

import java.util.Vector;

/* Object to manage lines in the autoexec.bat The lines get removed from
 * the file if the object gets destroyed. The environment is updated
 * as well if the line set a a variable */
public class AutoexecObject {
    private static Vector autoexec_strings = new Vector();
    private boolean installed = false;
    private String buf;

    public static void Shutdown() {
        autoexec_strings = new Vector();
    }

    public void Install(String in) {
        if(installed) Log.exit("autoexec: already created "+buf);
        installed = true;
        buf = in;
        autoexec_strings.add(buf);
        CreateAutoexec();

        //autoexec.bat is normally created AUTOEXEC_Init.
        //But if we are already running (first_shell)
        //we have to update the envirionment to display changes

        if(Shell.first_shell!=null)	{
            if (buf.startsWith("set ")) {
                String env = buf.substring(4);
                int pos = env.indexOf("=");
                if (pos<0) {
                    Shell.first_shell.SetEnv(env, "");
                } else {
                    Shell.first_shell.SetEnv(env.substring(0, pos), env.substring(pos+1));
                }

            }
        }
    }

    public void InstallBefore(String in) {
        if(installed) Log.exit("autoexec: already created "+buf);
        installed = true;
        buf = in;
        autoexec_strings.add(buf);
        CreateAutoexec();
    }

    // :TODO: when would this be necessary
//    public void close() {
//        if(!installed) return;
//
//        // Remove the line from the autoexecbuffer and update environment
//        for(auto_it it = autoexec_strings.begin(); it != autoexec_strings.end(); ) {
//            if((*it) == buf) {
//                it = autoexec_strings.erase(it);
//                std::string::size_type n = buf.size();
//                char* buf2 = new char[n + 1];
//                safe_strncpy(buf2, buf.c_str(), n + 1);
//                // If it's a environment variable remove it from there as well
//                if((strncasecmp(buf2,"set ",4) == 0) && (strlen(buf2) > 4)){
//                    char* after_set = buf2 + 4;//move to variable that is being set
//                    char* test = strpbrk(after_set,"=");
//                    if(!test) continue;
//                    *test = 0;
//                    //If the shell is running/exists update the environment
//                    if(first_shell) first_shell->SetEnv(after_set,"");
//                }
//                delete [] buf2;
//            } else it++;
//        }
//        CreateAutoexec();
//    }

    public static StringBuffer autoexec_data = new StringBuffer();
    private void CreateAutoexec() {
        /* Remove old autoexec.bat if the shell exists */
        if(Shell.first_shell!=null) Drive_virtual.VFILE_Remove("AUTOEXEC.BAT");
        autoexec_data = new StringBuffer();
        for (int i=0;i<autoexec_strings.size();i++) {
            String s= (String)autoexec_strings.elementAt(i);
            autoexec_data.append(s);
            autoexec_data.append("\r\n");
        }
        if (Shell.first_shell != null) {
            byte[] b = autoexec_data.toString().getBytes();
            Drive_virtual.VFILE_Register("AUTOEXEC.BAT",b,b.length);
        }
    }
}
