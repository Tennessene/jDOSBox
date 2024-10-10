package jdos.win.utils;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinSystem;

import java.util.Vector;

public class StringUtil extends WinAPI {
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

    static public String getStringW(int address) {
        StringBuffer result = new StringBuffer();
        while (true) {
            char c = (char)Memory.mem_readw(address);
            address+=2;
            if (c == 0)
                break;
            result.append(c);
        }
        return result.toString();
    }

    static public String getString(int address, int count) {
        if (count == -1)
            return getString(address);
        StringBuffer result = new StringBuffer();
        for (int i=0;i<count;i++) {
            char c = (char)Memory.mem_readb(address++); // :TODO: need to research converting according to 1252
            result.append(c);
        }
        return result.toString();
    }

    static public String getStringW(int address, int count) {
        if (count == -1)
            return getStringW(address);
        StringBuffer result = new StringBuffer();
        for (int i=0;i<count;i++) {
            char c = (char)Memory.mem_readw(address);
            address+=2;
            result.append(c);
        }
        return result.toString();
    }

    static public int strrchr(int address, int c) {
        int len = strlenA(address);
        for (int i=len-1;i>=0;i++) {
            if (readb(address+i)==c)
                return address+i;
        }
        return 0;
    }

    static public void strcat(int address, int address2) {
        int len = strlenA(address);
        strcpy(address+len, address2);
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
    static public int strncpy(int address, String value, int count) {
        byte[] b = value.getBytes();
        if (b.length+1<count)
            count = b.length+1;
        Memory.mem_memcpy(address, b, 0, count-1);
        Memory.mem_writeb(address+count-1, 0);
        return count-1;
    }
    static public int strncpy(int address, int address2, int count) {
        int i;
        for (i=0;i<count-1;i++) {
            int c = Memory.mem_readb(address2+i);
            if (c == 0)
                break;
            Memory.mem_writeb(address+i, c);
        }
        Memory.mem_writeb(address+count-1, 0);
        return i;
    }

    static public int strncmp(int s1, int s2, int count) {
        for (int i=0;i<count;i++) {
            int c1 = Memory.mem_readb(s1+i);
            int c2 = Memory.mem_readb(s2+i);
            if (c1 == c2) {
                if (c1 == 0)
                    return 0;
            } else if (c1<c2)
                return -1;
            else
                return 1;
        }
        return 0;
    }
    static public int strncmp(int s1, String s2, int count) {
        for (int i=0;i<count && i<s2.length();i++) {
            int c1 = Memory.mem_readb(s1+i);
            int c2 = s2.charAt(i);
            if (c1 == c2) {
                if (c1 == 0)
                    return 0;
            } else if (c1<c2)
                return -1;
            else
                return 1;
        }
        return 0;
    }
    static public int strcmp(int s1, int s2) {
        while (true) {
            int c1 = Memory.mem_readb(s1++);
            int c2 = Memory.mem_readb(s2++);

            if (c1<c2)
                return -1;
            else if (c1>c2)
                return 1;

            if (c1 == 0 && c1 == c2) {
                return 0;
            }
            if (c1 == 0)
                return -1;
            if (c2 == 0)
                return 1;
        }
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

    static public int allocateTempA(String s) {
        if (s == null)
            return 0;
        byte[] b = s.getBytes();
        int address = getTempBuffer(b.length+1);
        strcpy(address, s);
        return address;
    }
}
