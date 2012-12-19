package jdos.util;

public class HomeDirectory {
    static public String get() {
        String result = System.getenv("USERPROFILE");
        if (result == null) {
            result = System.getenv("HOMEPATH");
            if (result != null) {
                String drive = System.getenv("HOMEDRIVE");
                if (drive != null) {
                    result = drive+result;
                }
            }
        }
        if (result == null)
            result = System.getenv("HOME");
        if (result == null)
            result = System.getProperty("user.home");
        if (result.endsWith("\\") || result.endsWith("/")) {
            result = result.substring(0, result.length()-1);
        }
        if (result.length()==0)
            result=".";
        return result;
    }
}
