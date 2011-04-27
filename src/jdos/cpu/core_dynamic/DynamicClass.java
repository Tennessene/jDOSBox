package jdos.cpu.core_dynamic;

public abstract class DynamicClass {
    public DynDecode decode;
    public int call() {
        Cache.cache.block.running=decode.block;
        return call2();
    }
    abstract public int call2();
}
