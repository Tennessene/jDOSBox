package jdos.win.utils;

public class Path {
    public Path(String nativePath, String winPath) {
        this.nativePath = nativePath;
        this.winPath = winPath;
    }
    public String nativePath;
    public String winPath;
}
