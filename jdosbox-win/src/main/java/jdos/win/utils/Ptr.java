package jdos.win.utils;

public class Ptr {
    static public String toString(int v) {
        return Long.toString(v & 0xFFFFFFFFl, 16);
    }
}
