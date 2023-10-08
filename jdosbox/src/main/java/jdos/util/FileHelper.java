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
    
    public static void deleteFile(File path) {
        if( path.exists() ) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFile(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        path.delete();
    }
}
