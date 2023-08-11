package jdos.util;

import java.io.File;

public class FileHelper {
    public static String getHomeDirectory() {
        return HomeDirectory.get();
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
