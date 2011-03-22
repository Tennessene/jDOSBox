package jdos.misc;

import jdos.Dosbox;

public class Log {
    static public void log_msg(String format, Object ... args) {
        System.out.println(String.format(format, args));
    }
    static public void exit(String format, Object ... args) {
        System.out.print(String.format(format, args));
        try {
            throw new Exception("Stacktrace");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!Dosbox.applet)
            System.exit(0);
        else
            throw new RuntimeException("Exit");
    }
    static public void log(int type, int severity, String format, Object ... args) {
        if (severity>0)
            System.out.println(String.format(format, args));
    }
}
