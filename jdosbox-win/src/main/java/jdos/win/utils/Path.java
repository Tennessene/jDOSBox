package jdos.win.utils;

public class Path {
    public Path(String nativePath, String winPath) {
        this.nativePath = nativePath;
        this.winPath = winPath;
    }
    public final String nativePath;
    public final String winPath;
}
