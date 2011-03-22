package jdos.util;

import java.util.Vector;

public class StringHelper {
    public static void strreplace(byte[] b, char old, char n) {
        for (int i=0;i<b.length;i++) {
            if (b[i]==0)
                break;
            if (b[i] == old)
                b[i] = (byte)n;
        }
    }
    public static boolean isalpha(char c) {
        return ((c>='a' && c<='z') || (c>='A' && c<='Z'));
    }

    public static boolean isdigit(char c) {
        return ((c>='0' && c<='9'));
    }
    
    public static String toString(byte[] b) {
        return new String(b, 0, strlen(b));
    }
    public static String toString(byte[] b, int off, int len) {
        return new String(b, off, Math.min(len,strlen(b,off)));
    }

    public static void strcpy(byte[] b, int offset, byte[] b1, int offset2) {
        int len = strlen(b1, offset2);
        for (int i=0;i<len;i++)
            b[i+offset] = b1[i+offset2];
        b[offset+len]=0;
    }
    
    public static void strcpy(byte[] b, int offset, String s) {
        System.arraycopy(s.getBytes(), 0, b, offset, s.length());
        b[s.length()+offset]=0;
    }
    public static void strcpy(byte[] b, String s) {
        if (s == null) {
            b[0] = 0;
        } else {
            System.arraycopy(s.getBytes(), 0, b, 0, s.length());
            b[s.length()]=0;
        }
    }

    public static boolean isspace(char c) {
        return c==' ' || c=='\t' || c=='\n' || c==0x0b || c==0x0c || c=='\r';
    }
    
    public static int strlen(byte[] b) {
        for (int i=0;i<b.length;i++)
            if (b[i]==0)
                return i;
        return b.length;
    }

    public static int strncmp(byte[] s1, int off, byte[] s2, int off2, int len) {
        for (int i=0;i<s1.length && i<s2.length && i<len;i++) {
            if (s1[i+off] > s2[i+off2])
                return 1;
            if (s1[i+off] < s2[i+off2])
                return -1;
        }
        if (s1.length-off>=len && s2.length-off2>=len)
            return 0;
        if (s1.length-off>s2.length-off2)
            return 1;
        if (s2.length-off2>s1.length-off)
            return -1;
        return 0;
    }

    public static int memcmp(byte[] s1, byte[] s2, int len) {
        for (int i=0;i<s1.length && i<s2.length && i<len;i++) {
            if (s1[i] > s2[i])
                return 1;
            if (s1[i] < s2[i])
                return -1;
        }
        if (s1.length>=len && s2.length>=len)
            return 0;
        if (s1.length>s2.length)
            return 1;
        if (s2.length>s1.length)
            return -1;
        return 0;
    }

    public static int strlen(byte[] b, int off) {
        for (int i=off;i<b.length;i++)
            if (b[i]==0)
                return i-off;
        return b.length-off;
    }

    public static byte[] getDosString(String str, int len) {
        byte[] temp = new byte[len];
        System.arraycopy(str.getBytes(), 0, temp, 0, Math.min(str.length(), len));
        temp[temp.length-1]=0;
        return temp;
    }

    public static String replace(final String aInput, final String aOldPattern, final String aNewPattern){
        if ( aOldPattern.equals("") ) {
            throw new IllegalArgumentException("Old pattern must have content.");
        }

        final StringBuffer result = new StringBuffer();
        //startIdx and idxOld delimit various chunks of aInput; these
        //chunks always end where aOldPattern begins
        int startIdx = 0;
        int idxOld = 0;
        while ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {
            //grab a part of aInput which does not include aOldPattern
            result.append( aInput.substring(startIdx, idxOld) );
            //add aNewPattern to take place of aOldPattern
            result.append( aNewPattern );

            //reset the startIdx to just after the current match, to see
            //if there are any further matches
            startIdx = idxOld + aOldPattern.length();
        }
        //the final chunk will go to the end of aInput
        result.append( aInput.substring(startIdx) );
        return result.toString();
    }

    public static String[] split(final String input, String delimiter) {
        if (input != null && input.length() > 0) {
            int index1 = 0;
            int index2 = input.indexOf(delimiter);
            Vector result = new Vector();
            while (index2 >= 0) {
                String token = input.substring(index1, index2);
                result.addElement(token);
                index1 = index2 + delimiter.length();
                index2 = input.indexOf(delimiter, index1);
            }
            if (index1 <= input.length() - 1) {
                result.addElement(input.substring(index1));
            }
            String[] sda = new String[result.size()];
            result.copyInto(sda);
            return sda;
        }
        return new String[0];
    }

    public static String[] splitWithQuotes(final String input, char delimiter) {
        if (input != null && input.length() > 0) {
            StringBuffer part = new StringBuffer();
            boolean quote = false;
            Vector result = new Vector();

            for (int i=0;i<input.length();i++) {
                char c = input.charAt(i);
                if (quote) {
                    if (c == '\"') {
                        quote = false;
                    } else {
                        part.append(c);
                    }
                } else if (c == '\"') {
                    quote = true;
                } else if (c == delimiter) {
                    result.add(part.toString());
                    part = new StringBuffer();
                } else {
                    part.append(c);
                }
            }
            result.addElement(part.toString());
            String[] sda = new String[result.size()];
            result.copyInto(sda);
            return sda;
        }
        return new String[0];
    }
}
