package jdos.util;

public class IntPtr extends Ptr {
    // :TODO: maybe change Ptr so that IntPtr can use int[] directly?
    public IntPtr(int[] data) {
        super(new byte[data.length*4],0);
        for (int i=0;i<data.length;i++) {
            set(i, data[i]);
        }
    }
     public IntPtr(int size) {
        super(size);
    }
    public IntPtr(byte[] p, int off) {
        super(p, off);
    }
    public IntPtr(Ptr p, int off) {
        super(p, off);
    }
    public IntPtr(Ptr p) {
        super(p, 0);
    }
    public int dataWidth() {
        return 4;
    }
    public void set(int off, int val) {
        writed((int)off, val);
    }
    public int get(int off) {
        return (int)readd(off);
    }
    public void set(int off, long val) {
        writed(off, val);
    }
}
