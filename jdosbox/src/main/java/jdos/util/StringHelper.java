package jdos.util;

import java.util.Vector;

public class StringHelper {
    static public String StripWord(StringRef line) {
        String scan=line.value;
        scan=scan.trim();
        if (scan.startsWith("\"")) {
            int end_quote=scan.indexOf('"',1);
            if (end_quote>=0) {
                line.value=scan.substring(end_quote+1).trim();
                return scan.substring(1, end_quote);
            }
        }
        for (int i=0;i<scan.length();i++) {
            if (StringHelper.isspace(scan.charAt(i))) {
                line.value = scan.substring(i).trim();
                return scan.substring(0, i);
            }
        }
        line.value = "";
        return scan;
    }

    public static String sprintf(String format, Object[] args) {
        int pos = format.indexOf('%');
        if (pos>=0) {
            StringBuffer buffer = new StringBuffer();
            int argIndex = 0;
            while (pos>=0) {
                buffer.append(format.substring(0, pos));
                if (pos+1<format.length()) {
                    char c = format.charAt(++pos);
                    if (c == '%') {
                        buffer.append("%");
                        format = format.substring(2);
                    } else {
                        boolean leftJustify = false;
                        boolean showPlus = false;
                        boolean spaceSign = false;
                        boolean prefix = false;
                        boolean leftPadZero = false;
                        int width = 0;
                        int precision = -1;
                        boolean longValue = false;
                        boolean shortValue = false;

                        // flags
                        while (true) {
                            if (c=='-') {
                                leftJustify = true;
                            } else if (c=='+') {
                                showPlus = true;
                            } else if (c==' ') {
                                spaceSign = true;
                            } else if (c=='#') {
                                prefix = true;
                            } else if (c=='0') {
                                leftPadZero = true;
                            } else {
                                break;
                            }
                            if (pos+1<format.length()) {
                                c = format.charAt(++pos);
                            } else {
                                return buffer.toString();
                            }
                        }

                        // width
                        String w = "";
                        while (true) {
                            if (c>='0' && c<='9') {
                                w+=c;
                            } else {
                                break;
                            }
                            if (pos+1<format.length()) {
                                c = format.charAt(++pos);
                            } else {
                                return buffer.toString();
                            }
                        }
                        if (w.length()>0) {
                            width = Integer.parseInt(w);
                        }

                        // precision
                        if (c=='.') {
                            if (pos+1<format.length()) {
                                c = format.charAt(++pos);
                            } else {
                                return buffer.toString();
                            }

                            String p = "";
                            while (true) {
                                if (c>='0' && c<='9') {
                                    p+=c;
                                } else {
                                    break;
                                }
                                if (pos+1<format.length()) {
                                    c = format.charAt(++pos);
                                } else {
                                    return buffer.toString();
                                }
                            }
                            if (p.length()>0) {
                                precision = Integer.parseInt(p);
                            }
                        }

                        // length
                        if (c=='h') {
                            shortValue = true;
                            if (pos+1<format.length()) {
                                c = format.charAt(++pos);
                            } else {
                                return buffer.toString();
                            }
                        } else if (c=='l') {
                            longValue = true;
                            if (pos+1<format.length()) {
                                c = format.charAt(++pos);
                            } else {
                                return buffer.toString();
                            }
                        } else if (c=='L') {
                            longValue = true;
                            if (pos+1<format.length()) {
                                c = format.charAt(++pos);
                            } else {
                                return buffer.toString();
                            }
                        }

                        String value = "";
                        String strPrfix = "";
                        boolean negnumber = false;
                        if (c == 'c') {
                            if (args[argIndex] instanceof Character) {
                                value = String.valueOf(args[argIndex]);
                            } else if (args[argIndex] instanceof String) {
                                value = (String)args[argIndex];
                            } else {
                                System.out.println("Invalid printf argument type for %c: "+args[argIndex].getClass());
                                return buffer.toString();
                            }
                            if (value.length()>1)
                                value = value.substring(0,1);
                        } else if (c == 's') {
                            if (args[argIndex] instanceof Character) {
                                value = String.valueOf(args[argIndex]);
                            } else if (args[argIndex] instanceof String) {
                                value = (String)args[argIndex];
                            } else {
                                System.out.println("Invalid printf argument type for %s: "+args[argIndex].getClass());
                                return buffer.toString();
                            }
                            if (precision>0 && value.length()>precision) {
                                value = value.substring(0,precision);
                            }
                        } else if (c == 'x') {
                            if (args[argIndex] instanceof Integer) {
                                value = Integer.toString(((Integer)args[argIndex]).intValue(), 16);
                            } else if (args[argIndex] instanceof Long) {
                                value = Long.toString(((Long)args[argIndex]).longValue(), 16);
                            } else {
                                System.out.println("Invalid printf argument type for %x: "+args[argIndex].getClass());
                                return buffer.toString();
                            }
                            negnumber = value.startsWith("-");
                            if (negnumber)
                                value = value.substring(1);
                            if (precision==0 && value.equals("0")) {
                                format = format.substring(pos);
                                continue;
                            }
                            if (prefix) {
                                strPrfix += "0x"+value;
                            }
                        } else if (c == 'X') {
                            if (args[argIndex] instanceof Integer) {
                                value = Integer.toString(((Integer)args[argIndex]).intValue(), 16);
                            } else if (args[argIndex] instanceof Long) {
                                value = Long.toString(((Long)args[argIndex]).longValue(), 16);
                            } else {
                                System.out.println("Invalid printf argument type for %X: "+args[argIndex].getClass());
                                return buffer.toString();
                            }
                            negnumber = value.startsWith("-");
                            if (negnumber)
                                value = value.substring(1);
                            if (precision==0 && value.equals("0")) {
                                format = format.substring(pos);
                                continue;
                            }
                            if (precision>0) {
                                while (value.length()<precision) {
                                    value = "0"+value;
                                }
                            }
                            value = value.toUpperCase();
                            if (prefix) {
                                strPrfix += "0X"+value;
                            }
                        } else if (c == 'd') {
                            if (args[argIndex] instanceof Integer) {
                                value = Integer.toString(((Integer)args[argIndex]).intValue());
                            } else if (args[argIndex] instanceof Long) {
                                value = String.valueOf(((Long)args[argIndex]).longValue());
                            } else {
                                System.out.println("Invalid printf argument type for %d: "+args[argIndex].getClass());
                                return buffer.toString();
                            }
                            negnumber = value.startsWith("-");
                            if (negnumber)
                                value = value.substring(1);
                            if (precision==0 && value.equals("0")) {
                                format = format.substring(pos);
                                continue;
                            }
                            if (precision>0) {
                                while (value.length()<precision) {
                                    value = "0"+value;
                                }
                            }
                        } else if (c == 'f') {
                            if (args[argIndex] instanceof Double) {
                                value = String.valueOf(((Double)args[argIndex]).doubleValue());
                            } else if (args[argIndex] instanceof Float) {
                                value = String.valueOf(((Float)args[argIndex]).doubleValue());
                            } else {
                                System.out.println("Invalid printf argument type for %f: "+args[argIndex].getClass());
                                return buffer.toString();
                            }
                            negnumber = value.startsWith("-");
                            if (negnumber)
                                value = value.substring(1);
                            int dec = value.indexOf('.');
                            if (dec>=0) {
                                if (precision==0) {
                                    value = value.substring(0, dec);
                                } else if (value.length()>dec+1+precision) {
                                    value = value.substring(0, dec+1+precision);
                                }
                            }
                        }

                        if (negnumber) {
                            strPrfix = "-";
                        } else {
                            if (showPlus) {
                                strPrfix = "+"+strPrfix;
                            } else if (spaceSign) {
                                strPrfix = " "+strPrfix;
                            }
                        }
                        while (width>strPrfix.length()+value.length()) {
                            if (leftPadZero) {
                                strPrfix+="0";
                            } else if (leftJustify) {
                                value=value+" ";
                            } else {
                                strPrfix=" "+strPrfix;
                            }
                        }
                        buffer.append(strPrfix);
                        buffer.append(value);
                        format = format.substring(++pos);
                    }
                }
                argIndex++;
                pos = format.indexOf('%');
            }
            buffer.append(format);
            return buffer.toString();
        } else {
            return format;
        }
    }
    public static String leftJustify(String value, int places) {
        while (value.length()<places)
            value+=" ";
        return value;
    }
    public static String format(int d, int rightJustify) {
        String result = Integer.toString(d);
        while (result.length()<rightJustify) {
            result=" "+result;
        }
        return result;
    }
    public static String format(double d, int places) {
        String result = String.valueOf(d);
        int pos = result.indexOf('.');
        if (pos<0) {
            if (places>0) {
                result += ".";
                for (int i=0;i<places;i++) {
                    result += "0";
                }
            }
        } else {
            if (places == 0) {
                result = result.substring(0, pos);
            } else if (pos+places+1<result.length()) {
                result = result.substring(0, pos+places+1);
            }
        }
        return result;
    }
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
