package jdos.cpu;

public class PageFaultException extends RuntimeException {
    public boolean shouldRunException = true;
    public PageFaultException() {
    }
    public PageFaultException(boolean shouldRunException) {
        this.shouldRunException = shouldRunException;
    }
}
