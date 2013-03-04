package jdos.util;

import jdos.gui.MyActivity;

public class HomeDirectory {
    static public String get() {
        String result = MyActivity.activity.getFilesDir().getAbsolutePath();
        if (result.endsWith("\\") || result.endsWith("/")) {
            result = result.substring(0, result.length()-1);
        }
        return result;
    }

    public static String getVolumeLabel(String path) {
        return "";
    }
}
