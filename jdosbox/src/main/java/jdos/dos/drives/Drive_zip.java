package jdos.dos.drives;

import jdos.dos.*;
import jdos.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The ZIP image file system for jDosBox.
 * Usage: imgmount d game.zip -t zip
 * @author Petr Sladek alias slady
 */
public class Drive_zip extends Dos_Drive {

	private static final int SIZE_SECTOR = 512;
	private static final int SIZE_CLUSTER = 32;
	private ZipFile zipFile;
    private int totalSize;
	private Map<Short, FileSearch> dirSearchEntries = new HashMap<Short, FileSearch>();
	private final Map<String, ZipFileEntry> upperCaseNameFiles = new HashMap<String, ZipFileEntry>();
	private final Map<String, List<ZipFileEntry>> directoryStructureMap = new HashMap<String, List<ZipFileEntry>>();

	static public class FileSearch {
		private final Iterator<ZipFileEntry> fileListIterator;
		private final String nameRegexp;

		public FileSearch(final Iterator<ZipFileEntry> fileListIterator, final String namePattern) {
			this.fileListIterator = fileListIterator;
			final String regexp = namePattern.replace(".", "\\.").replace('?', '.').replace("*", ".*");
			if (regexp.endsWith("\\..*")) {
				this.nameRegexp = regexp.substring(0, regexp.length() - 4) + "(\\..*)?";
			} else {
				this.nameRegexp = regexp;
			}
		}

		public Iterator<ZipFileEntry> getFileListIterator() {
			return fileListIterator;
		}

		public String getNameRegexp() {
			return nameRegexp;
		}
	}

	static public class ZipFileEntry {
		private final ZipEntry zipEntry;
		private final String fullName;
		private final String fileName;
		private final String dirName;
		private final int dosDate;
		private final int dosTime;

		public ZipFileEntry(final ZipEntry zipEntry) {
			this.zipEntry = zipEntry;
			fullName = zipEntry.getName().toUpperCase().replace('/', '\\');

			// parse name and directory
			final String parsedFileName;
			if (isDirectory()) {
				parsedFileName = fullName.substring(0, fullName.length() - 1);
			} else {
				parsedFileName = fullName;
			}

			final int lastSlash = parsedFileName.lastIndexOf('\\');
			final int fileNameStart;
			final int dirNameEnd;
			if (lastSlash < 0) {
				fileNameStart = 0;
				dirNameEnd = 0;
			} else {
				fileNameStart = lastSlash + 1;
				dirNameEnd = lastSlash;
			}

			fileName = parsedFileName.substring(fileNameStart);
			dirName = parsedFileName.substring(0, dirNameEnd);

			// parse date and time
			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(zipEntry.getTime());

			final int day = calendar.get(Calendar.DAY_OF_MONTH);
			final int month = calendar.get(Calendar.MONTH) + 1;
			final int year = calendar.get(Calendar.YEAR);
			final int hour = calendar.get(Calendar.HOUR_OF_DAY);
			final int minute = calendar.get(Calendar.MINUTE);

			dosTime = ((minute & 0x003f) << 5) + (hour << 11);
			dosDate = (day & 0x001f) + ((month & 0x000f) << 5) + ((year - 1980) << 9);
		}

		public String getFullName() {
			return fullName;
		}

		public boolean isDirectory() {
			return zipEntry.isDirectory();
		}

		public String getDirName() {
			return dirName;
		}

		public String getFileName() {
			return fileName;
		}

		public int getDosDate() {
			return dosDate;
		}

		public int getDosTime() {
			return dosTime;
		}

		public long getSize() {
			return zipEntry.getSize();
		}

		public ZipEntry getZipEntry() {
			return zipEntry;
		}
	}

    public class Zip_File extends DOS_File {
    	private static final int CACHE_SHIFT = 13;
    	private static final int CACHE_MASK = 0x1FFF;
    	private static final int CACHE_PAGE_SIZE = 1 << CACHE_SHIFT; // 8192

    	private int seek, length;
        private final ZipEntry zipEntry;
        private InputStream is;
        private int real_pos=0;

        private final LRUCache cache = new LRUCache(32); // 256k

        public Zip_File(final String name, final ZipEntry zipEntry) {
        	this.name = name;
        	this.seek = 0;
        	this.length = (int) zipEntry.getSize();
        	this.zipEntry = zipEntry;
            try {
                this.is = zipFile.getInputStream(zipEntry);
            } catch (Exception e) {
                this.is = null;
            }
            open = true;
        }

        private byte[] fill(int offset) throws IOException {
            int skip;
            if (real_pos == offset) {
                skip=0;
            } else if (offset>real_pos) {
                skip = offset-real_pos;
            } else {
                is.close();
                is = zipFile.getInputStream(zipEntry);
                skip = offset;
            }
            while (skip>0) {
                skip-=is.skip(skip);
            }
            real_pos = offset;

            int todo = CACHE_PAGE_SIZE;
            byte[] b = new byte[CACHE_PAGE_SIZE];
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

        public boolean Read(byte[] b,/*Bit16u*/IntRef size) {
            if (is == null)
                return false;
            if (seek+size.value>length) {
                size.value = length-seek;
            }
            int len = size.value;
            int off = 0;
            while (len>0) {
                int offset = seek & ~CACHE_MASK;
                int index = seek & CACHE_MASK;
                int todo = len;
                if (todo > CACHE_PAGE_SIZE - index) {
                    todo = CACHE_PAGE_SIZE - index;
                }
                try {
                    byte[] d = get(offset);
                    System.arraycopy(d, index, b, off, todo);
                } catch (IOException e) {
                    return false;
                }
                seek+=todo;
                len-=todo;
                off+=todo;
            }
            return true;
        }

        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
        	Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            return false;
        }

        public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
            /*Bit32s*/
            int seekto = 0;

            switch (type) {
                case Dos_files.DOS_SEEK_SET:
                    seekto = (/*Bit32s*/int) pos.value;
                    break;
                case Dos_files.DOS_SEEK_CUR:
                    /* Is this relative seek signed? */
                    seekto = (/*Bit32s*/int) pos.value + (/*Bit32s*/int) seek;
                    break;
                case Dos_files.DOS_SEEK_END:
                    seekto = (/*Bit32s*/int) length + (/*Bit32s*/int) pos.value;
                    break;
            }

            if ((/*Bit32u*/long) seekto > length) seekto = (/*Bit32s*/int) length;
            if (seekto < 0) seekto = 0;
            seek = seekto;
            pos.value = seek;
            return true;
        }

        public boolean Close() {
        	length = 0;
        	if (is != null) {
                try {is.close();} catch (Exception e1) {}
                is = null;
            }
        	open = false;
            return false;
        }

        public /*Bit16u*/int GetInformation() {
            return 0;
        }
    }

    public Drive_zip(final String sysFilename) {
    	System.out.println("Drive_zip: "+sysFilename);
    	try {
			zipFile = new ZipFile(sysFilename);
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                this.totalSize += zipEntry.getSize();
				final ZipFileEntry zipFileEntry = new ZipFileEntry(zipEntry);
				upperCaseNameFiles.put(zipFileEntry.getFullName().toUpperCase(), zipFileEntry);
				final String dirName = zipFileEntry.getDirName().toUpperCase();

				if (!directoryStructureMap.containsKey(dirName)) {
					directoryStructureMap.put(dirName, new ArrayList<ZipFileEntry>());
				}

				directoryStructureMap.get(dirName).add(zipFileEntry);
			}
		} catch (IOException e) {
			Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
		}
    }

    public boolean AllocationInfo(/*Bit16u*/IntRef _bytes_sector,/*Bit8u*/ShortRef _sectors_cluster,/*Bit16u*/IntRef _total_clusters,/*Bit16u*/IntRef _free_clusters) {
        _bytes_sector.value = SIZE_SECTOR;
        _sectors_cluster.value = SIZE_CLUSTER;
        _total_clusters.value = totalSize / SIZE_SECTOR / SIZE_CLUSTER + 1;
        _free_clusters.value = 0;
    	return true;
    }

    public boolean isRemote() {
        return false;
    }

    public boolean isRemovable() {
        return false;
    }

    public /*Bits*/int UnMount() {
    	if (zipFile != null) {
    		try {
				zipFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
        return 0;
    }

    public /*Bit8u*/short GetMediaByte() {
        return 0;
    }

    public DOS_File FileCreate(String name,/*Bit16u*/int attributes) {
    	Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return null;
    }

    public boolean FileExists(String name) {
    	final ZipFileEntry zipFileEntry = upperCaseNameFiles.get(name);

    	if (zipFileEntry == null) {
    		return false;
    	}

        return true;
    }

    public DOS_File FileOpen(final String name,/*Bit32u*/int flags) {
        final ZipFileEntry zipFileEntry = upperCaseNameFiles.get(name);

        if (zipFileEntry == null) {
        	return null;
        }

        return new Zip_File(name, zipFileEntry.getZipEntry());
    }

    public boolean FileStat(String name, FileStat_Block stat_block) {
        /* TODO: Stub */
        return false;
    }

    public boolean FileUnlink(String name) {
        return false;
    }

    public boolean FindFirst(String dirName, Dos_DTA dta, boolean fcb_findfirst/*=false*/) {
    	final List<ZipFileEntry> fileList = directoryStructureMap.get(dirName);
    	final Iterator<ZipFileEntry> fileListIterator = fileList.iterator();
		ShortRef attrs = new ShortRef();
        StringRef pattern = new StringRef();
    	dta.GetSearchParams(attrs, pattern);
    	final FileSearch fileSearch = new FileSearch(fileListIterator, pattern.value);
		dirSearchEntries.put(attrs.value, fileSearch);
    	return findFile(dta);
    }

    public boolean FindNext(Dos_DTA dta) {
    	return findFile(dta);
    }

	private boolean findFile(final Dos_DTA dta) {
		ShortRef attrs = new ShortRef();
        StringRef pattern = new StringRef();
    	dta.GetSearchParams(attrs, pattern);
    	final FileSearch fileSearch = dirSearchEntries.get(attrs.value);

    	ZipFileEntry zipFileEntry = null;
    	final Iterator<ZipFileEntry> fileListIterator = fileSearch.getFileListIterator();

    	while (zipFileEntry == null && fileListIterator.hasNext()) {
    		final ZipFileEntry testZipFileEntry = fileListIterator.next();

    		if (testZipFileEntry.getFileName().matches(fileSearch.getNameRegexp())) {
    			zipFileEntry = testZipFileEntry;
    		}
    	}

    	if (zipFileEntry == null) {
			return false;
		}

    	final short attr;
    	if (zipFileEntry.isDirectory()) {
    		attr = Dos_system.DOS_ATTR_DIRECTORY;
    	} else {
    		attr = 0;
    	}

    	dta.SetResult(zipFileEntry.getFileName(), zipFileEntry.getSize(),
    			zipFileEntry.getDosDate(), zipFileEntry.getDosTime(), attr);
		return true;
	}

    public boolean GetFileAttr(final String name,/*Bit16u*/IntRef attr) {
    	final ZipFileEntry zipFileEntry = upperCaseNameFiles.get(name);

    	if (zipFileEntry == null) {
    		return false;
    	}

        attr.value=Dos_system.DOS_ATTR_ARCHIVE;
    	if (zipFileEntry.isDirectory()) {
    		attr.value = Dos_system.DOS_ATTR_DIRECTORY;
    	}
        return true;
    }

    public boolean MakeDir(String dir) {
        return false;
    }

    public boolean RemoveDir(String dir) {
        return false;
    }

    public boolean Rename(String oldname, String newname) {
        return false;
    }

    public boolean TestDir(final String dir) {
    	if (dir.isEmpty()) {
    		// root directory
    		return true;
    	}

    	final ZipFileEntry zipFileEntry = upperCaseNameFiles.get(dir + '\\');

    	if (zipFileEntry == null) {
    		return false;
    	}

    	return zipFileEntry.isDirectory();
    }

}
