package jdos.cpu;

public class LazyFlags {
    public LazyFlags() {
    }

    public LazyFlags(LazyFlags in) {
        this.copy(in);
    }

    public void copy(LazyFlags in) {
        var1 = in.var1;
        var2 = in.var2;
        res=in.res;
        type = in.type;
        prev_type = in.prev_type;
        oldcf = in.oldcf;
    }
    public int var1;
    public int var2;
    public int res;
    public int type;
    public int prev_type;
    public boolean oldcf;
}
