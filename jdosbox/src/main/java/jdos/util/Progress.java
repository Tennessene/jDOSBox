package jdos.util;

public interface Progress {
    public void set(int value);
    public void status(String value);
    public void done();
    public boolean hasCancelled();
    public void speed(String value);
    public void initializeSpeedValue(long totalExpected);
    public void incrementSpeedValue(long value);
}
