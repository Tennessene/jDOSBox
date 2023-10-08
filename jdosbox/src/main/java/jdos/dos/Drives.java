package jdos.dos;

import jdos.misc.setup.Section;
import jdos.util.StringHelper;
import jdos.util.StringRef;

public class Drives {
    public static boolean WildFileCmp(String file, String wild) 
    {
        StringBuilder file_name;
        StringBuilder file_ext;
        StringBuilder wild_name;
        StringBuilder wild_ext;
        int pos;

        pos = file.indexOf('.');
        if (pos>=0) {
            file_name = new StringBuilder(file.substring(0, pos));
            file_ext = new StringBuilder(file.substring(pos + 1));
        } else {
            file_name = new StringBuilder(file);
            file_ext = new StringBuilder();
        }
        file_name = new StringBuilder(file_name.toString().toUpperCase());
        file_ext = new StringBuilder(file_ext.toString().toUpperCase());
        pos = wild.indexOf('.');
        if (pos>=0) {
            wild_name = new StringBuilder(wild.substring(0, pos));
            wild_ext = new StringBuilder(wild.substring(pos + 1));
        } else {
            wild_name = new StringBuilder(wild);
            wild_ext = new StringBuilder();
        }
        wild_name = new StringBuilder(wild_name.toString().toUpperCase());
        wild_ext = new StringBuilder(wild_ext.toString().toUpperCase());
        while (wild_name.length()<8) wild_name.append(' ');
        while (wild_ext.length()<3) wild_ext.append(' ');
        while (file_name.length()<8) file_name.append(' ');
        while (file_ext.length()<3) file_ext.append(' ');
        if (wild_name.length()>8) wild_name = new StringBuilder(wild_name.substring(0, 8));
        if (wild_ext.length()>3) wild_name = new StringBuilder(wild_ext.substring(0, 3));
        if (file_name.length()>8) wild_name = new StringBuilder(file_name.substring(0, 8));
        if (file_ext.length()>3) wild_name = new StringBuilder(file_ext.substring(0, 3));
        /* Names are right do some checking */
        for (int i=0;i<wild_name.length() && i<file_name.length();i++) {
            if (wild_name.charAt(i)=='*') break;
            if (wild_name.charAt(i)!='?' && wild_name.charAt(i)!=file_name.charAt(i)) return false;
        }
        for (int i=0;i<wild_ext.length() && i<file_ext.length();i++) {
            if (wild_ext.charAt(i)=='*') break;
            if (wild_ext.charAt(i)!='?' && wild_ext.charAt(i)!=file_ext.charAt(i)) return false;
        }
        return true;
    }

    public static void Set_Label(String input, StringRef result, boolean cdrom) {
        /*Bitu*/int togo     = 8;
        /*Bitu*/int vnamePos = 0;
        /*Bitu*/int labelPos = 0;
        /*Bitu*/boolean point    = false;
        byte[] output = new byte[13];
        //spacepadding the filenamepart to include spaces after the terminating zero is more closely to the specs. (not doing this now)
        // HELLO\0' '' '

        while (togo > 0) {
            if (vnamePos>=input.length() || input.charAt(vnamePos)==0) break;
            if (!point && (input.charAt(vnamePos)=='.')) {	togo=4; point=true; }

            //another mscdex quirk. Label is not always uppercase. (Daggerfall)
            output[labelPos] = (byte)(cdrom?input.charAt(vnamePos):input.toUpperCase().charAt(vnamePos));

            labelPos++; vnamePos++;
            togo--;
            if (vnamePos<input.length() && (togo==0) && !point) {
                if (input.charAt(vnamePos)=='.') vnamePos++;
                output[labelPos]='.'; labelPos++; point=true; togo=3;
            }
        }
        output[labelPos]=0;

        //Remove trailing dot. except when on cdrom and filename is exactly 8 (9 including the dot) letters. MSCDEX feature/bug (fifa96 cdrom detection)
        if((labelPos > 0) && (output[labelPos-1] == '.') && !(cdrom && labelPos ==9))
            output[labelPos-1] = 0;
        result.value = StringHelper.toString(output);
    }
    
    public static final Section.SectionFunction DRIVES_Init = section -> DriveManager.Init(section);
}
