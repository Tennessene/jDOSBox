package jdos.debug;

import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogTypes;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class Debug_gui {
    static class _LogGroup {
        String front;
        boolean enabled;
    }
    static _LogGroup[] loggrp = new _LogGroup[LogTypes.LOG_MAX];
    static OutputStream debuglog;

    static Section.SectionFunction LOG_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            if (debuglog != null) {
                try {debuglog.close();} catch (Exception e){}
            }
        }
    };
    static Section.SectionFunction LOG_Init = new Section.SectionFunction() {
        public void call(Section section) {
            Section_prop sect=(Section_prop)section;
            String blah = sect.Get_string("logfile");
            if(blah != null && blah.length()!=0){
                try {
                    debuglog = new FileOutputStream(blah);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sect.AddDestroyFunction(LOG_Destroy);
            for (int i=1;i<LogTypes.LOG_MAX;i++) {
                loggrp[i].enabled=sect.Get_bool(loggrp[i].front.toLowerCase());
            }
        }
    };
    public static void LOG_StartUp() {
        for (int i=0;i<loggrp.length;i++)
            loggrp[i] = new _LogGroup();                
        /* Setup logging groups */
        loggrp[LogTypes.LOG_ALL].front="ALL";
        loggrp[LogTypes.LOG_VGA].front="VGA";
        loggrp[LogTypes.LOG_VGAGFX].front="VGAGFX";
        loggrp[LogTypes.LOG_VGAMISC].front="VGAMISC";
        loggrp[LogTypes.LOG_INT10].front="INT10";
        loggrp[LogTypes.LOG_SB].front="SBLASTER";
        loggrp[LogTypes.LOG_DMACONTROL].front="DMA_CONTROL";

        loggrp[LogTypes.LOG_FPU].front="FPU";
        loggrp[LogTypes.LOG_CPU].front="CPU";
        loggrp[LogTypes.LOG_PAGING].front="PAGING";

        loggrp[LogTypes.LOG_FCB].front="FCB";
        loggrp[LogTypes.LOG_FILES].front="FILES";
        loggrp[LogTypes.LOG_IOCTL].front="IOCTL";
        loggrp[LogTypes.LOG_EXEC].front="EXEC";
        loggrp[LogTypes.LOG_DOSMISC].front="DOSMISC";

        loggrp[LogTypes.LOG_PIT].front="PIT";
        loggrp[LogTypes.LOG_KEYBOARD].front="KEYBOARD";
        loggrp[LogTypes.LOG_PIC].front="PIC";

        loggrp[LogTypes.LOG_MOUSE].front="MOUSE";
        loggrp[LogTypes.LOG_BIOS].front="BIOS";
        loggrp[LogTypes.LOG_GUI].front="GUI";
        loggrp[LogTypes.LOG_MISC].front="MISC";

        loggrp[LogTypes.LOG_IO].front="IO";

        /* Register the log section */
//        Section_prop sect= Dosbox.control.AddSection_prop("log",LOG_Init);
//        Prop_string Pstring = sect.Add_string("logfile", Property.Changeable.Always,"");
//        Pstring.Set_help("file where the log messages will be saved to");
//        for (int i=1;i<LogTypes.LOG_MAX;i++) {
//            Prop_bool Pbool = sect.Add_bool(loggrp[i].front.toLowerCase(),Property.Changeable.Always,true);
//            Pbool.Set_help("Enable/Disable logging of this type.");
//        }
//        Msg.add("LOG_CONFIGFILE_HELP","Logging related options for the debugger.\n");
    }
}
