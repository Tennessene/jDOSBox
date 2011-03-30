package jdos.util;

import jdos.gui.Main;
import jdos.Dosbox;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

public class FileIOFactory {
    static public final int MODE_READ = 0x01;
    static public final int MODE_WRITE = 0x02;
    static public final int MODE_TRUNCATE = 0x04;

    static private class RandomIO extends RandomAccessFile implements FileIO {
        File file;
        public RandomIO(File file, String mode)throws FileNotFoundException {
            super(file, mode);
            this.file = file;
        }
        public long lastModified() {
            return file.lastModified();
        }
    }

    static private class RamIO implements FileIO {
        private byte[] data;
        private int pos=0;
        private int mode;

        public RamIO(byte[] data, int mode) {
            this.data = data;
            this.mode = mode;
        }
        public int read() throws IOException {
            if (pos>=data.length)
                return -1;
            return data[pos++];
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null)
                throw new NullPointerException();
            if (off<0 || off+len>=data.length)
                throw new IndexOutOfBoundsException();

            if (pos>=data.length)
                return -1;
            if (pos+len>data.length)
                len = data.length-pos;
            System.arraycopy(data, pos, b, off, len);
            pos+=len;
            return len;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int skipBytes(int n) throws IOException {
            if (pos>=data.length)
                return 0;
            if (n+pos>data.length)
                n = data.length-pos;
            pos+=n;
            return n;
        }

        public void write(int b) throws IOException {
            if ((mode & MODE_WRITE)==0)
                throw new IOException("Read Only");
            if (pos>=data.length)
                throw new IOException("EOF");
            data[pos++] = (byte)(b & 0xFF);
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if ((mode & MODE_WRITE)==0)
                throw new IOException("Read Only");
            if (pos+len>data.length)
                throw new IOException("EOF");
            System.arraycopy(b, off, data, pos, len);
            pos+=len;
        }

        public void seek(long p) throws IOException {
            if (p>data.length)
                throw new IOException("EOF");
            if (p<0)
                throw new IOException("Invalid position: "+p);
            pos=(int)p;
        }

        public long length() throws IOException {
            return data.length;
        }

        public void setLength(long newLength) throws IOException {
            throw new IOException("Not Supported");
        }
        
        public void close() throws IOException {
        }

        public long getFilePointer() throws IOException {
            return pos;
        }

        public long lastModified() {
            return 0;
        }
    }
    
    static private class JarIO implements FileIO {
        private String path;
        private int pos=0;
        private int real_pos=0;
        private int mode;
        private int len;
        private InputStream is;
        final private int cacheShift = 13;
        final private int cacheMask = 0x1FFF;
        final private int cachePageSize = 1 << cacheShift; // 8192
        // Necessary for the thrashing that happens when a zip (i.e. wad file, is inside the zipped jar)
        final private LRUCache cache = new LRUCache(32); // 256k

        private InputStream getIS() {
            return Dosbox.class.getResourceAsStream(path);
        }
        public JarIO(String path, int mode) {
            this.path = path;
            this.mode = mode;

            is = getIS();
            try {
                len = is.available();
            } catch (Exception e) {
            }
        }
        private byte[] fill(int offset) throws IOException {
            int skip;
            if (real_pos == offset) {
                skip=0;
            } else if (offset>real_pos) {
                skip = offset-real_pos;
            } else {
                is.close();
                is = getIS();
                skip = offset;
            }
            while (skip>0) {
                skip-=is.skip(skip);
            }
            real_pos = offset;

            int todo = cachePageSize;
            byte[] b = new byte[cachePageSize];
            int done = 0;
            while (todo>0) {
                int r=is.read(b, done, todo);
                if (r<0) {
                    break;
                }
                done+=r;
                todo-=r;
            }
            real_pos += done; 
            return b;
        }

        private byte[] get(int offset) throws IOException {
            Integer i = new Integer(offset);
            byte[] b = (byte[])cache.get(i);
            if (b == null) {
                b = fill(offset);
                cache.put(i, b);
            }
            return b;
        }

        public int read() throws IOException {
            if (pos>=len)
                return -1;
            int offset = pos >> cacheShift;
            int index = pos & cacheMask;
            byte[] b = get(offset);
            pos++;
            return (b[index] & 0xFF);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null)
                throw new NullPointerException();
            if (off<0 || off+len>=this.len)
                throw new IndexOutOfBoundsException();

            if (pos>=this.len)
                return -1;
            if (pos+len>this.len)
                len = this.len-pos;
            int result = len;
            while (len>0) {
                int offset = pos & ~cacheMask;
                int index = pos & cacheMask;
                int todo = len;
                if (todo > cachePageSize - index) {
                    todo = cachePageSize - index;
                }
                byte[] d = get(offset);
                System.arraycopy(d, index, b, off, todo);
                pos+=todo;
                len-=todo;
                off+=todo;
            }
            return result;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int skipBytes(int n) throws IOException {
            if (pos>=len)
                return 0;
            if (n+pos>len)
                n = len-pos;
            pos+=n;
            return n;
        }

        public void write(int b) throws IOException {
            skipBytes(1);
            int ii=0;
//            if ((mode & MODE_WRITE)==0)
//                throw new IOException("Read Only");
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            skipBytes(len);
            int ii=0;
//            if ((mode & MODE_WRITE)==0)
//                throw new IOException("Read Only");
//            if (pos+len>data.length)
//                throw new IOException("EOF");
//            System.arraycopy(b, off, data, pos, len);
//            pos+=len;
        }

        public void seek(long p) throws IOException {
            if (p == pos) {
                return;
            }
            if (p>len)
                throw new IOException("EOF");
            if (p<0)
                throw new IOException("Invalid position: "+p);
            pos = (int)p;
        }

        public long length() throws IOException {
            return len;
        }

        public void setLength(long newLength) throws IOException {
            throw new IOException("Not Supported");
        }

        public void close() throws IOException {
            is.close();
        }

        public long getFilePointer() throws IOException {
            return pos;
        }

        public long lastModified() {
            return 0;
        }
    }
    static public boolean isRemote(String path) {
        return (path.toLowerCase().startsWith("http://") || path.toLowerCase().startsWith("jar://"));
    }
    static private class MyByteArrayOutputStream extends ByteArrayOutputStream {
        public MyByteArrayOutputStream(int size) {
            super(size);
        }
        public byte[] getBuf() {
            return buf;
        }
    }
    static public boolean canOpen(String path, int mode) {
        try {
            FileIO f = open(path, mode);
            f.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    static public FileIO open(String path, int mode) throws FileNotFoundException {
        if (path.toLowerCase().startsWith("http://")) {
            try {
                URL url = new URL(path);
                URLConnection urlConn = url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setUseCaches(true);
                byte[] b = null;
                long size = 0;
                InputStream is = urlConn.getInputStream();
                ByteArrayOutputStream os;
                if (path.toLowerCase().endsWith(".zip")) {
                    ZipInputStream zis = new ZipInputStream(is);
                    ZipEntry entry = zis.getNextEntry();
                    is = zis;
                    os = new MyByteArrayOutputStream((int)entry.getSize());
                    b = ((MyByteArrayOutputStream)os).getBuf();
                    size = entry.getSize();
                } else {
                    size = urlConn.getContentLength();
                    os = new ByteArrayOutputStream();
                }
                int read;
                byte[] buffer = new byte[8096];
                String msg = "Downloading "+path.substring(path.lastIndexOf('/')+1);
                Main.showProgress(msg, 0);
                long completed = 0;
                while (true) {
                    read = is.read(buffer);
                    if (read<=0)
                        break;
                    os.write(buffer, 0, read);
                    completed+=read;
                    Main.showProgress(msg, (int)(completed*100/size));
                }
                is.close();
                if (b == null)
                    b = os.toByteArray();
                return new RamIO(b, mode);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Main.showProgress(null, 0);
            }
            throw new FileNotFoundException(path);
        } else if (path.toLowerCase().startsWith("jar://")) {
            path = path.substring(6);
            URL url = Dosbox.class.getResource(path);

            InputStream is = Dosbox.class.getResourceAsStream(path);
            if (is == null) {
                return null;
            }
            try {is.close();} catch (Exception e) {}
            return new JarIO(path, mode);
        } else {
            File f = new File(path);
            String m = "r";
            if ((mode & MODE_WRITE)!=0)
                m+="w";
            if ((mode & MODE_TRUNCATE)!=0) {
                if (f.exists())
                    f.delete();
            } else if (!f.exists()) {
                throw new FileNotFoundException();
            }
            return new RandomIO(f, m);
        }
    }

}
