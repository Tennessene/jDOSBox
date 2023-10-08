package jdos.util;

import java.io.IOException;

public interface FileIO {
    int read() throws IOException;
    int read(byte[] b, int off, int len) throws IOException;
    int read(byte[] b) throws IOException;
    int skipBytes(int n) throws IOException;
    void write(int b) throws IOException;
    void write(byte[] b) throws IOException;
    void write(byte[] b, int off, int len) throws IOException;
    void seek(long pos) throws IOException;
    long length() throws IOException;
    void setLength(long newLength) throws IOException;
    void close() throws IOException;
    long getFilePointer() throws IOException;
    long lastModified();
}
