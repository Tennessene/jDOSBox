package jdos.util;

public class StringRef {
    public StringRef() {}
    public StringRef(String value) {
        this.value = value;
    }
    public String value;
    public String toString() {
        throw new RuntimeException("Ooops");
    }
}
