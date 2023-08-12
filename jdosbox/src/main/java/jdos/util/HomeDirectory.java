package jdos.util;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeDirectory {
    static final Pattern p = Pattern.compile( " \\([A-Za-z]:\\)" );

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

    public static String getVolumeLabel(String path)
    {
        File file = new File(path);
        while (file.getParentFile()!=null) {
            file = file.getParentFile();
        }
        FileSystemView v = FileSystemView.getFileSystemView();
        final String full = v.getSystemDisplayName(file);
        final int length = full.length();
        // Remove the trailing _(X:)
        final String chopped;

        final Matcher m = p.matcher( full );
        if ( m.find() ) {
            chopped = full.substring( 0, m.start() ).trim();
        }
        else {
            chopped = full.trim();
        }
        return chopped;
    }
}
