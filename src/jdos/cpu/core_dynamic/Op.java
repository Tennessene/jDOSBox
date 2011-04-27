package jdos.cpu.core_dynamic;

abstract public class Op {
    public int c=-1;
    public Op next;
    abstract public int call();
}
