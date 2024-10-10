package jdos.misc.setup;

import jdos.misc.Cross;
import jdos.misc.Log;
import jdos.misc.Msg;
import jdos.util.FileIOFactory;
import jdos.util.StringHelper;

import java.io.*;
import java.util.Vector;

public class Config {
    static public final String MAJOR_VERSION = "0.74";
    static public final String VERSION = "0.74.30";
    static public final boolean C_DYNAMIC = true;
    static public final boolean C_DYNREC = false;
    static public final boolean C_FPU = true;
    static public final boolean C_IPX = true;
    static public final boolean C_NE2000 = true;
    static public final boolean C_DEBUG = false;
    static public final boolean C_HEAVY_DEBUG = false;
    static public final boolean USE_FULL_TLB = true;
    static public final boolean C_VGARAM_CHECKED = true;    
    static public final boolean DEBUG_LOG = false;
    static public final boolean DEBUG_DEDERMINISTIC = false;
    static public final boolean IPX_DEBUGMSG = false;
    static public final boolean DYNAMIC_CORE_VERIFY = false;
    static public final boolean FAST_STRINGS = true;
    static public final boolean PCI_FUNCTIONALITY_ENABLED = true;

    static String current_config_dir; // Set by parseconfigfile so Prop_path can use it to construct the realpath
    static public interface StartFunction {
        public void call();
    }
    public CommandLine cmdline;

    private Vector sectionlist = new Vector();
    private boolean secure_mode; //Sandbox mode
    private StartFunction _start_function;

    public Config(CommandLine cmd) {
        cmdline = cmd;
        secure_mode = false;
    }

    public Section_line AddSection_line(String _name, Section.SectionFunction _initfunction) {
        Section_line blah = new Section_line(_name);
        blah.AddInitFunction(_initfunction);
        sectionlist.add(blah);
        return blah;
    }

    public Section_prop AddSection_prop(String _name, Section.SectionFunction _initfunction) {
        return AddSection_prop(_name, _initfunction, false);
    }
    public Section_prop AddSection_prop(String _name, Section.SectionFunction _initfunction, boolean canchange) {
        Section_prop blah = new Section_prop(_name);
        blah.AddInitFunction(_initfunction, canchange);
        sectionlist.add(blah);
        return blah;
    }
    public Section GetSection(int index) {
        if (index>=0 && index< sectionlist.size())
            return (Section)sectionlist.elementAt(index);
        return null;
    }
    public Section GetSection(String _sectionname) {
        for (int i=0;i<sectionlist.size();i++) {
            Section s = (Section)sectionlist.elementAt(i);
            if (s.GetName().equalsIgnoreCase(_sectionname)) return s;
        }
        return null;
    }
    public Section GetSectionFromProperty(String prop) {
        for (int i=0;i<sectionlist.size();i++) {
            Section s = (Section)sectionlist.elementAt(i);
            if (!s.GetPropValue(prop).equals(Section.NO_SUCH_PROPERTY)) return s;
        }
        return null;
    }
    public void SetStartUp(StartFunction _function) {
        _start_function = _function;
    }
    public void Init() {
        for (int i=0;i<sectionlist.size();i++) {
            Section s = (Section)sectionlist.elementAt(i);
            s.ExecuteInit();
        }
    }
    public void Destroy() {
        for (int i=sectionlist.size()-1;i>=0;i--) {
            Section s = (Section)sectionlist.elementAt(i);
            s.ExecuteDestroy(true);
        }
    }
    public void StartUp() {
        _start_function.call();
    }
    private void fprintf(OutputStream outfile, String format, String args, int maxwidth) throws IOException {
        format = StringHelper.replace(format, "%s", args);
        if (maxwidth>0) {
            while (args.length()<maxwidth) {
                args = " "+args;
            }
            format = StringHelper.replace(format, "%"+maxwidth+"s", args);
        }
        fputs(format, outfile);
    }

    static public void fputs(String str, OutputStream outfile) throws IOException {
        if (Cross.isWindows()) {
            str = StringHelper.replace(str, "\n", "\r\n");
        }
        outfile.write(str.getBytes());
    }

    public boolean PrintConfig(String configfilename) {
        FileOutputStream outfile = null;
        try {
            outfile = new FileOutputStream(configfilename);
            fprintf(outfile, Msg.get("CONFIGFILE_INTRO")+"\n", VERSION, 0);
            for (int k=0;k<sectionlist.size();k++) {
                Section tel = (Section)sectionlist.elementAt(k);
                Section_prop sec = null;
                if (tel instanceof Section_prop)
                    sec = (Section_prop)tel;
                fputs("["+tel.GetName().toLowerCase()+"]", outfile);
                if (sec != null) {
                    int maxwidth = 0;
                    int i = 0;
                    Property p;

                    while ((p = sec.Get_prop(i++)) != null) {
                        maxwidth = Math.max(maxwidth, p.propname.length());
                    }
                    String prefix = "\n# %"+(maxwidth>0?String.valueOf(maxwidth):"")+"s";
                    String prefix2 = "\n#   ";
                    for (int l=0;l<maxwidth;l++) {
                        prefix2 = prefix2+" ";
                    }
                    i = 0;
                    while ((p = sec.Get_prop(i++)) != null) {
                        String help = p.Get_help();
                        help = StringHelper.replace(help, "\n", prefix2);
                        fprintf(outfile,  prefix+": "+help,p.propname, maxwidth);
                        Vector values = p.GetValues();
                        if (!values.isEmpty()) {
                            fputs(prefix2+Msg.get("CONFIG_SUGGESTED_VALUES"), outfile);
                            for (int j=0;j<values.size();j++) {
                                Value v = (Value)values.elementAt(j);
                                if (!v.toString().equals("%u")) {
                                    if (j!=0)
                                        fputs(",", outfile);
                                    fputs(" "+v.toString(), outfile);
                                }
                            }
                            fputs(".", outfile);
                        }
                    }
                    fputs("\n", outfile);
                } else {
                    String help = "# "+Msg.get(tel.GetName().toUpperCase()+"_CONFIGFILE_HELP");
                    StringHelper.replace(help, "\n", "\n# ");
                    fputs(help, outfile);
                }
                fputs("\n", outfile);
                tel.PrintData(outfile);
                fputs("\n", outfile); /* Always an empty line between sections */
            }
            return true;
        } catch (FileNotFoundException e) {

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outfile != null) {
                try { outfile.close(); } catch (Exception e){}
            }
        }
        return false;
    }

    private static boolean first_configfile = true;

    public boolean ParseConfigFile(String configfilename) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(FileIOFactory.openStream(configfilename)));
            String settings_type = first_configfile?"primary":"additional";
            first_configfile = false;
            Log.log_msg("CONFIG:Loading "+settings_type+" settings from config file "+configfilename);
            current_config_dir = FileIOFactory.getFullPath(configfilename);
            String line;
            Section currentsection = null;
            while ((line=in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0)
                    continue;
                char c = line.charAt(0);
                if (c == '%' || c == '\0' || c == '#' || c == ' ' || c == '\n')
                    continue;
                if (c == '[') {
                    int pos = line.indexOf(']');
                    if (pos < 0) continue;
                    String sec = line.substring(1, pos);
                    Section testsec = GetSection(sec);
                    if (testsec != null) {
                        currentsection = testsec;
                    }
                } else {
                    if (currentsection != null)
                        currentsection.HandleInputline(line);
                }
            }
            return true;
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {in.close();} catch (Exception e) {}

        }
        current_config_dir = ""; //So internal changes don't use the path information
        return false;
    }
    public void ParseEnv() {
//        if (!Dosbox.applet) {
//            Map env = System.getenv();
//            Set keyset = env.keySet();
//            Iterator it = keyset.iterator();
//            while (it.hasNext()) {
//                String envName = (String)it.next();
//                if (envName.startsWith("DOSBOX_")) {
//                    String sec_name = envName.substring(7);
//                    int pos = sec_name.indexOf("_");
//                    if (pos>0) {
//                        String prop_name = sec_name.substring(pos+1);
//                        sec_name = sec_name.substring(0, pos);
//                        Section sect = GetSection(sec_name);
//                        if (sect != null) {
//                            sect.HandleInputline(prop_name+"="+env.get(envName));
//                        }
//                    }
//                }
//            }
//        }
    }
    public boolean SecureMode() {
        return secure_mode;
    }
    public void SwitchToSecureMode() { secure_mode = true; }//can't be undone
}
