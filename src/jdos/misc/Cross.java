package jdos.misc;

import jdos.Dosbox;
import jdos.misc.setup.Config;
import jdos.util.BooleanRef;
import jdos.util.FileHelper;
import jdos.util.StringRef;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cross {
    static final Pattern p = Pattern.compile( " \\([A-Za-z]:\\)" );

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf( "win" ) >= 0;
    }

    public static boolean isCDRom(String path) {
        String label = getVolumeLabel(path).toLowerCase();
        if (label.indexOf("bd-rom")>=0 || label.indexOf("dvd")>=0)
            return true;
        return false;
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

    static public class dir_information {
        private File[] list;
        private int index;
    }

    static public dir_information open_directory(String dirname) {
        if (!new File(dirname).exists()) return null;
        dir_information result = new dir_information();

        result.list = new File(dirname).listFiles();
        result.index = -1;
        return result;
    }

    static public boolean read_directory_first(dir_information dirp, StringRef entry_name, BooleanRef is_directory) {
        if (dirp.list.length == 0) return false;
        entry_name.value = dirp.list[0].getName();
        is_directory.value = dirp.list[0].isDirectory();
        dirp.index = 0;
        return true;
    }

    static public boolean read_directory_next(dir_information dirp, StringRef entry_name, BooleanRef is_directory) {
        if (dirp.index+1 >= dirp.list.length) return false;
        dirp.index++;
        entry_name.value = dirp.list[dirp.index].getName();
        is_directory.value = dirp.list[dirp.index].isDirectory();
        return true;
    }

    static public void close_directory(dir_information dirp) {
    }

    static public String ResolveHomedir(String temp_line) {
        return FileHelper.resolve_path(temp_line);
    }

    static public String CreatePlatformConfigDir() {
        if (!Dosbox.allPrivileges) return "";
        String result = System.getProperty("user.dir");
        if (result != null && result.length()!=0 && new File(result).exists()) {
            result += File.separator + ".dosbox";
            File f = new File(result);
            if (!f.exists()) {
                f.mkdir();
            }
            return f.getAbsolutePath()+File.separator;
        }
        return "";
    }

    static public String GetPlatformConfigName() {
        return "dosbox-"+ Config.MAJOR_VERSION +".conf"; 
    }

    static public void CreateDir(String dir) {
        new File(dir).mkdirs();
    }
}
