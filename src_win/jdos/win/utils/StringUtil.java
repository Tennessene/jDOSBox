package jdos.win.utils;

import jdos.cpu.CPU;
import jdos.hardware.Memory;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.WinSystem;

import java.util.Vector;

public class StringUtil {
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

    static public String format(String format, boolean wide, int argIndex) {
        int pos = format.indexOf('%');
        if (pos>=0) {
            StringBuffer buffer = new StringBuffer();
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
                            if (shortValue || wide || longValue)
                                value = new Character((char) (CPU.CPU_Peek32(argIndex) & 0xFFFF) ).toString();
                            else
                                value = new Character((char) (CPU.CPU_Peek32(argIndex) & 0xFF) ).toString();
                        } else if (c == 's') {
                            if (longValue || wide)
                                value = new LittleEndianFile(CPU.CPU_Peek32(argIndex)).readCStringW();
                            else
                                value = new LittleEndianFile(CPU.CPU_Peek32(argIndex)).readCString();

                            if (precision>0 && value.length()>precision) {
                                value = value.substring(0,precision);
                            }
                        } else if (c == 'S') {
                            if (longValue || !wide)
                                value = new LittleEndianFile(CPU.CPU_Peek32(argIndex)).readCStringW();
                            else
                                value = new LittleEndianFile(CPU.CPU_Peek32(argIndex)).readCString();
                            if (precision>0 && value.length()>precision) {
                                value = value.substring(0,precision);
                            }
                        } else if (c == 'x') {
                            if (longValue) {
                                long l = CPU.CPU_Peek32(argIndex) | CPU.CPU_Peek32(argIndex+1) << 32l;
                                argIndex++;
                                value = Long.toString(l, 16);
                            } else {
                                value = Integer.toString(CPU.CPU_Peek32(argIndex), 16);
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
                            if (longValue) {
                                long l = CPU.CPU_Peek32(argIndex) | CPU.CPU_Peek32(argIndex+1) << 32l;
                                argIndex++;
                                value = Long.toString(l, 16);
                            } else {
                                value = Integer.toString(CPU.CPU_Peek32(argIndex), 16);
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
                            if (longValue) {
                                long l = CPU.CPU_Peek32(argIndex) | CPU.CPU_Peek32(argIndex+1) << 32l;
                                argIndex++;
                                value = Long.toString(l, 10);
                            } else {
                                value = Integer.toString(CPU.CPU_Peek32(argIndex), 10);
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
                        } else if (c == 'u') {
                            if (longValue) {  // :TODO: not truly 64-bit unsigned
                                long l = CPU.CPU_Peek32(argIndex) | CPU.CPU_Peek32(argIndex+1) << 32l;
                                argIndex++;
                                value = Long.toString(l, 10);
                            } else {
                                value = Long.toString(CPU.CPU_Peek32(argIndex) & 0xFFFFFFFFl, 10);
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
                        }else if (c == 'f') {
                            value = Float.toString(Float.intBitsToFloat(CPU.CPU_Peek32(argIndex)));
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
    static public int strlenA(int str ) {
        int s = str;
        while (Memory.mem_readb(s)!=0) s++;
        return (s - str);
    }

    static public int strlenW(int str ) {
        int s = str;
        while (Memory.mem_readw(s)!=0) s+=2;
        return (s - str)/2;
    }

    static public String getString(int address) {
        StringBuffer result = new StringBuffer();
        while (true) {
            char c = (char)Memory.mem_readb(address++); // :TODO: need to research converting according to 1252
            if (c == 0)
                break;
            result.append(c);
        }
        return result.toString();
    }

    static public void strcpy(int address, String value) {
        byte[] b = value.getBytes();
        Memory.mem_memcpy(address, b, 0, b.length);
        Memory.mem_writeb(address+b.length, 0);
    }
    static public void strcpy(int address, int src) {
        int len =strlenA(src);
        Memory.mem_memcpy(address, src, len + 1);
    }
    static public void strncpy(int address, String value, int count) {
        byte[] b = value.getBytes();
        if (b.length+1<count)
            count = b.length+1;
        Memory.mem_memcpy(address, b, 0, count-1);
        Memory.mem_writeb(address+count-1, 0);
    }
    static public void strcpyW(int address, String value) {
        char[] c = value.toCharArray();
        for (int i=0;i<c.length;i++) {
            Memory.mem_writew(address, c[i]);
            address+=2;
        }
        Memory.mem_writew(address, 0);
    }
    static public void strncpyW(int address, String value, int count) {
        char[] c = value.toCharArray();
        if (c.length+1<count)
            count = c.length+1;
        for (int i=0;i<count-1;i++) {
            Memory.mem_writew(address, c[i]);
            address+=2;
        }
        Memory.mem_writew(address, 0);
    }
    static public char tolowerW(char w) {
        return new Character(w).toString().toLowerCase().charAt(0);
    }

    static public char toupperW(char w) {
        return new Character(w).toString().toUpperCase().charAt(0);
    }
    static public void _strupr(int str) {
        while (true) {
            char c = (char)Memory.mem_readb(str);
            if (c==0)
                break;
            c = toupperW(c);
            Memory.mem_writeb(str++, c);
        }
    }

    static public String[] parseQuotedString(String s) {
        s = s.trim();
        Vector results = new Vector();
        StringBuffer buffer = new StringBuffer();
        boolean quote = false;

        for (int i=0;i<s.length();i++) {
            char c = s.charAt(i);
            if (c == '\"') {
                quote = !quote;
            } else if (quote || c !=' ') {
                buffer.append(c);
            } else {
                results.add(buffer.toString());
                buffer = new StringBuffer();
            }
        }
        results.add(buffer.toString());
        String[] r = new String[results.size()];
        results.copyInto(r);
        return r;
    }

    static public int allocateA(String s) {
        if (s == null)
            return 0;
        byte[] b = s.getBytes();
        int address = WinSystem.getCurrentProcess().heap.alloc(b.length+1, false);
        strcpy(address, s);
        return address;
    }
}
