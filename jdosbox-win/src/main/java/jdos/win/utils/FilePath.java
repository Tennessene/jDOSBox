package jdos.win.utils;

import jdos.dos.DOS_File;
import jdos.dos.Dos_files;
import jdos.dos.Dos_system;
import jdos.dos.drives.Drive_fat;
import jdos.util.IntRef;
import jdos.util.LongRef;

import java.io.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class FilePath {
    static public Hashtable<String, Object> disks = new Hashtable<String, Object>();
    static Set<String> faked = new HashSet<String>();

    static {
        faked.add("\\windows\\system32\\dsound.vxd");
        faked.add("\\windows\\system32\\dsound.dll");
        faked.add("\\windows\\system32\\ddraw.dll");
        faked.add("\\windows\\system32\\ddhelp.exe");
        faked.add("\\windows\\system32\\");
        faked.add("\\windows\\");
    }
    public FilePath(String path) {
        this.path = path;
        String driveLetter = path.substring(0, 1).toUpperCase();
        Object drive = disks.get(driveLetter);
        if (drive instanceof String)
            this.delagate = new JavaPath(path);
        else if (drive instanceof Drive_fat)
            this.delagate = new FatPath((Drive_fat)drive, path);
    }

    public FilePath getParentFile() {
        return delagate.getParentFile();
    }

    public boolean exists() {
        boolean result = delagate.exists();
        if (!result) {
            int pos = path.toLowerCase().indexOf("\\windows\\");
            if (pos>=0)
                result = faked.contains(path.toLowerCase().substring(pos));
        }
        return result;
    }

    public String getName() {
        return delagate.getName();
    }

    public boolean mkdirs() {
        return delagate.mkdirs();
    }

    public boolean delete() {
        return delagate.delete();
    }

    public boolean createNewFile() {
        return delagate.createNewFile();
    }

    public FilePath[] listFiles(FileFilter filter) {
        return delagate.listFiles(filter);
    }

    public long lastModified() {
        return delagate.lastModified();
    }

    public long length() {
        return delagate.length();
    }

    public boolean isDirectory() {
        return delagate.isDirectory();
    }

    public boolean renameTo(FilePath path) {
        return delagate.renameTo(path);
    }

    public String getAbsolutePath() {
        return delagate.getAbsolutePath();
    }

    public InputStream getInputStream() {
        return delagate.getInputStream();
    }

    public boolean open(boolean write) {
        return delagate.open(write);
    }

    public void seek(long pos) {
        delagate.seek(pos);
    }

    public void skipBytes(int count) {
        delagate.skipBytes(count);
    }

    public long getFilePointer() {
        return delagate.getFilePointer();
    }

    public int read(byte[] buffer) {
        return delagate.read(buffer);
    }

    public void write(byte[] buffer) {
        delagate.write(buffer);
    }

    public void close() {
        delagate.close();
    }

    public String path;
    private FilePathInterface delagate;

    static private class FatPath implements FilePathInterface {
        String fullPath;
        String path;
        Drive_fat drive;
        DOS_File file;
        long length = 0;

        public FatPath(Drive_fat drive, String path) {
            this.fullPath = path;
            this.path = path.substring(3);
            this.drive = drive;
            file = drive.FileOpen(this.path, 0xFF);
            if (file != null) {
                LongRef pos = new LongRef(0);
                file.Seek(pos, Dos_files.DOS_SEEK_END);
                length = pos.value;
                pos.value = 0;
                file.Seek(pos, Dos_files.DOS_SEEK_SET);
            }
        }

        public FilePath getParentFile() {
            int pos = fullPath.lastIndexOf("\\");
            if (pos>=0)
                return new FilePath(fullPath.substring(0, pos+1));
            return null;
        }

        public boolean exists() {
            if (file != null)
                return true;
            return isDirectory();
        }

        public String getName() {
            int pos = fullPath.lastIndexOf("\\");
            return fullPath.substring(pos+1);
        }

        public boolean mkdirs() {
            FilePath parent = getParentFile();
            if (parent != null) {
                if (!parent.exists()) {
                    if (!parent.mkdirs())
                        return false;
                }
            }
            DOS_File file = drive.FileCreate(path, Dos_system.DOS_ATTR_DIRECTORY);
            if (file == null)
                return false;
            return true;
        }

        public boolean delete() {
            return drive.FileUnlink(path);
        }

        public boolean createNewFile() {
            DOS_File file =  drive.FileCreate(path, Dos_system.DOS_ATTR_ARCHIVE);
            if (file == null)
                return false;
            file.Close();
            return true;
        }

        public FilePath[] listFiles(FileFilter filter) {
            return new FilePath[0];
        }

        public long lastModified() {
            return 0;
        }

        public long length() {
            return length;
        }

        public boolean isDirectory() {
            return drive.TestDir(path);
        }

        public boolean renameTo(FilePath path) {
            return drive.Rename(this.path, path.path);
        }

        public String getAbsolutePath() {
            return path;
        }

        public InputStream getInputStream() {
            return new InputStream() {
                byte[] buf = new byte[1];

                public int read() throws IOException {

                    int result = FatPath.this.read(buf);
                    if (result == 1)
                        return buf[0] & 0xFF;
                    return result;
                }

                public void reset() {
                    FatPath.this.seek(0);
                }
            };
        }

        public boolean open(boolean write) {
            return file != null;
        }

        public void seek(long pos) {
            if (file != null) {
                LongRef ref = new LongRef(pos);
                file.Seek(ref, Dos_files.DOS_SEEK_SET);
            }
        }

        public void skipBytes(int count) {
            if (file != null) {
                LongRef ref = new LongRef(count);
                file.Seek(ref, Dos_files.DOS_SEEK_CUR);
            }
        }

        public long getFilePointer() {
            if (file != null) {
                LongRef ref = new LongRef(0);
                file.Seek(ref, Dos_files.DOS_SEEK_CUR);
                return ref.value;
            }
            return 0;
        }

        public int read(byte[] buffer) {
            if (file != null) {
                IntRef size = new IntRef(buffer.length);
                if (!file.Read(buffer, size))
                    return -1;
                return size.value;
            }
            return -1;
        }

        public void write(byte[] buffer) {
            if (file != null) {
                IntRef size = new IntRef(buffer.length);
                file.Write(buffer, size);
            }
        }

        public void close() {
            if (file != null) {
                file.Close();
            }
        }
    }
    static private class JavaPath implements FilePathInterface {
        File file;
        RandomAccessFile openFile;

        public boolean open(boolean write) {
            try {
                openFile = new RandomAccessFile(file, write?"rw":"r");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public void seek(long pos) {
            if (openFile != null) {
                try {
                    openFile.seek(pos);
                } catch (Exception e) {
                }
            }
        }

        public void skipBytes(int count) {
            if (openFile != null) {
                try {
                    openFile.skipBytes(count);
                } catch (Exception e) {
                }
            }
        }

        public long getFilePointer() {
            if (openFile != null) {
                try {
                    return openFile.getFilePointer();
                } catch (Exception e) {
                }
            }
            return 0;
        }

        public int read(byte[] buffer) {
            if (openFile != null) {
                try {
                    return openFile.read(buffer);
                } catch (Exception e) {
                }
            }
            return 0;
        }

        public void write(byte[] buffer) {
            if (openFile != null) {
                try {
                    openFile.write(buffer);
                } catch (Exception e) {
                }
            }
        }

        public void close() {
            if (openFile != null) {
                try {
                    openFile.close();
                } catch (Exception e) {
                }
                openFile = null;
            }
        }

        public JavaPath(String path) {
            file = new File(path);
        }
        public FilePath getParentFile() {
            return new FilePath(file.getParent());
        }

        public boolean exists() {
            return file.exists();
        }

        public String getName() {
            return file.getName();
        }

        public boolean mkdirs() {
            return file.mkdirs();
        }

        public boolean delete() {
            return file.delete();
        }

        public boolean createNewFile() {
            try {
                return file.createNewFile();
            } catch (Exception e) {
                return false;
            }
        }

        public FilePath[] listFiles(FileFilter filter) {
            File[] files = file.listFiles(filter);
            FilePath[] result = new FilePath[files.length];
            for (int i=0;i<files.length;i++)
                result[i] = new FilePath(files[i].getPath());
            return result;
        }

        public long lastModified() {
            return file.lastModified();
        }

        public long length() {
            return file.length();
        }

        public boolean isDirectory() {
            return file.isDirectory();
        }

        public boolean renameTo(FilePath path) {
            return file.renameTo(new File(path.path));
        }

        public String getAbsolutePath() {
            return file.getAbsolutePath();
        }

        public InputStream getInputStream() {
            try {
                return new FileInputStream(file);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static interface FilePathInterface {
        public FilePath getParentFile();
        public boolean exists();
        public String getName();
        public boolean mkdirs();
        public boolean delete();
        public boolean createNewFile();
        public FilePath[] listFiles(FileFilter filter);
        public long lastModified();
        public long length();
        public boolean isDirectory();
        public boolean renameTo(FilePath path);
        public String getAbsolutePath();
        public InputStream getInputStream();

        public boolean open(boolean write);
        public void seek(long pos);
        public void skipBytes(int count);
        public long getFilePointer();
        public int read(byte[] buffer);
        public void write(byte[] buffer);
        public void close();
    }
}
