package jdos.cpu.core_dynamic;

abstract public class Op {
    public int c=-1;
    public long eip;
    public Op next;
    abstract public int call();
}
