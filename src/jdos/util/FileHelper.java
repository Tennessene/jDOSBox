package jdos.util;

import java.io.File;

public class FileHelper {
    public static String getHomeDirectory() {
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
        return result;
    }
    public static String resolve_path(String path) {
        if (path.startsWith("~")) {
            return getHomeDirectory()+path.substring(1);
        }
        return path;
    }
    
    public static boolean deleteFile(File path) {
        if( path.exists() ) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteFile(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return(path.delete());
    }
}
