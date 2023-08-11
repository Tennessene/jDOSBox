package jdos.util;

public class IntRef {
    public IntRef(int value) {
        this.value = value;
    }
    public int value;
    public String toString() {
        throw new RuntimeException("Ooops");
    }
}
