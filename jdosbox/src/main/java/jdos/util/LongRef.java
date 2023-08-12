package jdos.util;

public class LongRef {
    public LongRef(long value) {
        this.value = value;
    }
    public long value;
    public String toString() {
        throw new RuntimeException("Ooops");
    }
}
