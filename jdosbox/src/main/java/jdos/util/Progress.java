package jdos.util;

public interface Progress {
    void set(int value);
    void status(String value);
    void done();
    boolean hasCancelled();
    void speed(String value);
    void initializeSpeedValue(long totalExpected);
    void incrementSpeedValue(long value);
}
