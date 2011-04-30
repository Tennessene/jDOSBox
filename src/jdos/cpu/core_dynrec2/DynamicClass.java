package jdos.cpu.core_dynrec2;

public abstract class DynamicClass {
    public DynDecode decode;
    public int call() {
        return call2();
    }
    abstract public int call2();
}
