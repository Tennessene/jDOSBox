package jdos.util;

import java.io.IOException;

public interface FileIO {
    public int read() throws IOException;
    public int read(byte b[], int off, int len) throws IOException;
    public int read(byte b[]) throws IOException;
    public int skipBytes(int n) throws IOException;
    public void write(int b) throws IOException;
    public void write(byte b[]) throws IOException;
    public void write(byte b[], int off, int len) throws IOException;
    public void seek(long pos) throws IOException;
    public long length() throws IOException;
    public void setLength(long newLength) throws IOException;
    public void close() throws IOException;
    public long getFilePointer() throws IOException;
    public long lastModified();
}
