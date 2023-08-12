package jdos.cpu;

public final class LazyFlags {
    public LazyFlags() {
        var1 = Flags.var1;
        var2 = Flags.var2;
        res = Flags.res;
        oldcf = Flags.oldcf;
        type = Flags.type;
    }

    public LazyFlags(LazyFlags in) {
        this.copy(in);
    }

    public void copy(LazyFlags in) {
        var1 = in.var1;
        var2 = in.var2;
        res=in.res;
        type = in.type;
        oldcf = in.oldcf;
    }
    public int var1;
    public int var2;
    public int res;
    public boolean oldcf;
    public Flags.GetFlags type = Flags.t_UNKNOWN;
}
