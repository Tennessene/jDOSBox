package jdos.dos.drives;

import jdos.dos.*;
import jdos.util.*;

public class Drive_iso extends Dos_Drive {
    static private final int ISO_MAX_HASH_TABLE_SIZE = 100;
    static private final int ISO_FRAMESIZE = 2048;

    static final private int ISO_ASSOCIATED	= 4;
    static final private int ISO_DIRECTORY = 2;
    static final private int ISO_HIDDEN = 1;
    static final private int ISO_MAX_FILENAME_LENGTH = 37;
    static final private int ISO_MAXPATHNAME = 256;
    static final private int ISO_FIRST_VD = 16;
    static private boolean IS_ASSOC(int fileFlags) {return (fileFlags & ISO_ASSOCIATED)!=0;}
    static private boolean IS_DIR(int fileFlags) {return (fileFlags & ISO_DIRECTORY)!=0;}
    static private boolean IS_HIDDEN(int fileFlags) {return (fileFlags & ISO_HIDDEN)!=0;}

    private static class isoDirEntry {
        /*Bit8u*/short length;
        /*Bit8u*/short extAttrLength;
        /*Bit32u*/long extentLocationL;
        /*Bit32u*/long extentLocationM;
        /*Bit32u*/long dataLengthL;
        /*Bit32u*/long dataLengthM;
        /*Bit8u*/short dateYear;
        /*Bit8u*/short dateMonth;
        /*Bit8u*/short dateDay;
        /*Bit8u*/short timeHour;
        /*Bit8u*/short timeMin;
        /*Bit8u*/short timeSec;
        /*Bit8u*/short timeZone;
        /*Bit8u*/short fileFlags;
        /*Bit8u*/short fileUnitSize;
        /*Bit8u*/short interleaveGapSize;
        /*Bit16u*/int VolumeSeqNumberL;
        /*Bit16u*/int VolumeSeqNumberM;
        /*Bit8u*/short fileIdentLength;
        /*Bit8u*/byte[] ident = new byte[222];

        public void copy(isoDirEntry i) {
            length = i.length;
            extAttrLength = i.extAttrLength;
            extentLocationL = i.extentLocationL;
            extentLocationM = i.extentLocationM;
            dataLengthL = i.dataLengthL;
            dataLengthM = i.dataLengthM;
            dateYear = i.dateYear;
            dateMonth = i.dateMonth;
            dateDay = i.dateDay;
            timeHour = i.timeHour;
            timeMin = i.timeMin;
            timeSec = i.timeSec;
            timeZone = i.timeZone;
            fileFlags = i.fileFlags;
            fileUnitSize = i.fileUnitSize;
            interleaveGapSize = i.interleaveGapSize;
            VolumeSeqNumberL = i.VolumeSeqNumberL;
            VolumeSeqNumberM = i.VolumeSeqNumberM;
            fileIdentLength = i.fileIdentLength;
            System.arraycopy(i.ident, 0, ident, 0, ident.length);
        }
        public void load(byte[] data, int offset, int len) {
            Ptr p = new Ptr(data, offset);
            length = p.readb(0);p.inc(1);
            extAttrLength = p.readb(0);p.inc(1);
            extentLocationL = p.readd(0);p.inc(4);
            extentLocationM = p.readd(0);p.inc(4);
            dataLengthL = p.readd(0);p.inc(4);
            dataLengthM = p.readd(0);p.inc(4);

            dateYear = p.readb(0);p.inc(1);
            dateMonth = p.readb(0);p.inc(1);
            dateDay = p.readb(0);p.inc(1);
            timeHour = p.readb(0);p.inc(1);
            timeMin = p.readb(0);p.inc(1);
            timeSec = p.readb(0);p.inc(1);
            timeZone = p.readb(0);p.inc(1);
            fileFlags = p.readb(0);p.inc(1);
            fileUnitSize = p.readb(0);p.inc(1);
            interleaveGapSize = p.readb(0);p.inc(1);
            VolumeSeqNumberL = p.readw(0);p.inc(2);
            VolumeSeqNumberM = p.readw(0);p.inc(2);
            fileIdentLength = p.readb(0);p.inc(1);
            if (length>33)
                p.read(ident, length-33);
        }
    }

    private static class DirIterator {
		boolean valid;
		boolean root;
		/*Bit32u*/long currentSector;
		/*Bit32u*/long endSector;
		/*Bit32u*/long pos;
	}
    private DirIterator[] dirIterators = new DirIterator[DOS_Drive_Cache.MAX_OPENDIRS];

	private int nextFreeDirIterator;

	private static class SectorHashEntry {
		boolean valid;
		/*Bit32u*/long sector;
		/*Bit8u*/byte[] data = new byte[ISO_FRAMESIZE];
	}
    private SectorHashEntry[] sectorHashEntries = new SectorHashEntry[ISO_MAX_HASH_TABLE_SIZE];

	private boolean dataCD;
	private isoDirEntry rootEntry = new isoDirEntry();
	private /*Bit8u*/short mediaid;
	private String fileName;
	private /*Bit8u*/short subUnit;
	private char driveLetter;
	private String discLabel;

    class isoFile extends DOS_File {
        isoFile(Drive_iso drive, String name, FileStat_Block stat, /*Bit32u*/long offset) {
            this.drive = drive;
            time = stat.time;
            date = stat.date;
            attr = stat.attr;
            fileBegin = offset;
            filePos = fileBegin;
            fileEnd = fileBegin + stat.size;
            cachedSector = -1;
            open = true;
            this.name = null;
            SetName(name);
        }
        public boolean	Read(byte[] data,/*Bit16u*/IntRef size) {
            if (filePos + size.value > fileEnd)
                size.value = (int)(fileEnd - filePos);

            /*Bit16u*/int nowSize = 0;
            int sector = (int)(filePos / ISO_FRAMESIZE);
            /*Bit16u*/int sectorPos = (/*Bit16u*/int)(filePos % ISO_FRAMESIZE);

            if (sector != cachedSector) {
                if (drive.readSector(buffer, sector)) cachedSector = sector;
                else { size.value = 0; cachedSector = -1; }
            }
            while (nowSize < size.value) {
                /*Bit16u*/int remSector = ISO_FRAMESIZE - sectorPos;
                /*Bit16u*/int remSize = size.value - nowSize;
                if(remSector < remSize) {
                    System.arraycopy(buffer, sectorPos, data, nowSize, remSector);
                    nowSize += remSector;
                    sectorPos = 0;
                    sector++;
                    cachedSector++;
                    if (!drive.readSector(buffer, sector)) {
                        size.value = nowSize;
                        cachedSector = -1;
                    }
                } else {
                    System.arraycopy(buffer, sectorPos, data, nowSize, remSize);
                    nowSize += remSize;
                }

            }

            size.value = nowSize;
            filePos += size.value;
            return true;
        }
        public boolean	Write(byte[] data,/*Bit16u*/IntRef size) {
            return false;
        }
        public boolean	Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
            int p = (int)(pos.value & 0xFFFFFFFFl);
            switch (type) {
                case Dos_files.DOS_SEEK_SET:
                    filePos = fileBegin + p;
                    break;
                case Dos_files.DOS_SEEK_CUR:
                    filePos += p;
                    break;
                case Dos_files.DOS_SEEK_END:
                    filePos = fileEnd + p;
                    break;
                default:
                    return false;
            }
            if (filePos > fileEnd || filePos < fileBegin)
                filePos = fileEnd;

            pos.value = filePos - fileBegin;
            return true;
        }
        public boolean	Close() {
            if (refCtr == 1) open = false;
	        return true;
        }
        public /*Bit16u*/int GetInformation() {
            return 0x40;		// read-only drive
        }

        private Drive_iso drive;
        private /*Bit8u*/byte[] buffer = new byte[ISO_FRAMESIZE];
        private int cachedSector;
        private /*Bit32u*/long fileBegin;
        private /*Bit32u*/long filePos;
        private /*Bit32u*/long fileEnd;
        private /*Bit32u*/int info;
    }

    public Drive_iso(char driveLetter, String fileName, /*Bit8u*/short mediaid, IntRef error) {
        nextFreeDirIterator = 0;

        this.fileName = fileName;
        ShortRef s = new ShortRef(subUnit);
        error.value = UpdateMscdex(driveLetter, fileName, s);
        subUnit = s.value;

        if (error.value == 0) {
            if (loadImage()) {
                info = "isoDrive "+fileName;
                this.driveLetter = driveLetter;
                this.mediaid = mediaid;
                StringRef buffer = new StringRef();
                if (!DosMSCDEX.MSCDEX_GetVolumeName(subUnit, buffer)) buffer.value="";
                StringRef d = new StringRef(discLabel);
                Drives.Set_Label(buffer.value,d,true);
                discLabel = d.value;
            } else if (CDROM_Interface_Image.images[subUnit].HasDataTrack() == false) { //Audio only cdrom
                info = "isoDrive "+fileName;
                this.driveLetter = driveLetter;
                this.mediaid = mediaid;
                StringRef d = new StringRef(discLabel);
                Drives.Set_Label("Audio_CD",d,true);
                discLabel = d.value;
            } else error.value = 6; //Corrupt image
        }
        for (int i=0;i<dirIterators.length;i++) {
            dirIterators[i] = new DirIterator();
        }
        for (int i=0;i<sectorHashEntries.length;i++) {
            sectorHashEntries[i] = new SectorHashEntry();
        }
    }

    int UpdateMscdex(char driveLetter, String path, /*Bit8u*/ShortRef subUnit) {
        if (DosMSCDEX.MSCDEX_HasDrive(driveLetter)) {
            CDROM_Interface_Image oldCdrom = CDROM_Interface_Image.images[subUnit.value];
            Dos_cdrom.CDROM_Interface cdrom = new CDROM_Interface_Image(subUnit.value);
            if (!cdrom.SetDevice(path, 0)) {
                CDROM_Interface_Image.images[subUnit.value] = oldCdrom;
                cdrom.close();
                return 3;
            }
            DosMSCDEX.MSCDEX_ReplaceDrive(cdrom, subUnit.value);
            return 0;
        } else {
            return DosMSCDEX.MSCDEX_AddDrive(driveLetter, path, subUnit);
        }
    }

    public void Activate() {
        ShortRef s = new ShortRef(subUnit);
        UpdateMscdex(driveLetter, fileName, s);
        subUnit = s.value;
    }

    public DOS_File FileOpen(String name,/*Bit32u*/int flags) {
        if ((flags & 0x0f) == Dos_files.OPEN_WRITE) {
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            return null;
        }

        isoDirEntry de = new isoDirEntry();
        boolean success = lookup(de, name) && !IS_DIR(de.fileFlags);

        if (success) {
            FileStat_Block file_stat = new FileStat_Block();
            file_stat.size = de.dataLengthL;
            file_stat.attr = Dos_system.DOS_ATTR_ARCHIVE | Dos_system.DOS_ATTR_READ_ONLY;
            file_stat.date = Dos.DOS_PackDate(1900 + de.dateYear, de.dateMonth, de.dateDay);
            file_stat.time = Dos.DOS_PackTime(de.timeHour, de.timeMin, de.timeSec);
            DOS_File file = new isoFile(this, name, file_stat, de.extentLocationL * ISO_FRAMESIZE);
            file.flags = flags;
            return file;
        }
        return null;
    }

    public DOS_File FileCreate(String name,/*Bit16u*/int attributes) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return null;
    }

    public boolean FileUnlink(String _name) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return false;
    }

    public boolean RemoveDir(String _dir) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return false;
    }

    public boolean MakeDir(String _dir) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return false;
    }

    public boolean TestDir(String dir) {
        isoDirEntry de=new isoDirEntry();
        return (lookup(de, dir) && IS_DIR(de.fileFlags));
    }

    public boolean FindFirst(String dir,Dos_DTA dta,boolean fcb_findfirst/*=false*/) {
        isoDirEntry de=new isoDirEntry();
        if (!lookup(de, dir)) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }

        // get a directory iterator and save its id in the dta
        int dirIterator = GetDirIterator(de);
        boolean isRoot = (dir.length() == 0);
        dirIterators[dirIterator].root = isRoot;
        dta.SetDirID(dirIterator);

        /*Bit8u*/ShortRef attr = new ShortRef();
        StringRef pattern = new StringRef();
        dta.GetSearchParams(attr, pattern);

        if (attr.value == Dos_system.DOS_ATTR_VOLUME) {
            dta.SetResult(discLabel, 0, 0, 0, (short)Dos_system.DOS_ATTR_VOLUME);
            return true;
        } else if ((attr.value & Dos_system.DOS_ATTR_VOLUME)!=0 && isRoot && !fcb_findfirst) {
            if (Drives.WildFileCmp(discLabel,pattern.value)) {
                // Get Volume Label (DOS_ATTR_VOLUME) and only in basedir and if it matches the searchstring
                dta.SetResult(discLabel, 0, 0, 0, (short)Dos_system.DOS_ATTR_VOLUME);
                return true;
            }
        }

        return FindNext(dta);
    }

    public boolean FindNext(Dos_DTA dta) {
        /*Bit8u*/ShortRef attr=new ShortRef(0);
        StringRef pattern = new StringRef();
        dta.GetSearchParams(attr, pattern);

        int dirIterator = dta.GetDirID();
        boolean isRoot = dirIterators[dirIterator].root;

        isoDirEntry de = new isoDirEntry();
        while (GetNextDirEntry(dirIterator, de)) {
            /*Bit8u*/short findAttr = 0;
            if (IS_DIR(de.fileFlags)) findAttr |= Dos_system.DOS_ATTR_DIRECTORY;
            else findAttr |= Dos_system.DOS_ATTR_ARCHIVE;
            if (IS_HIDDEN(de.fileFlags)) findAttr |= Dos_system.DOS_ATTR_HIDDEN;

            String deident = StringHelper.toString(de.ident);
            if (!IS_ASSOC(de.fileFlags) && !(isRoot && de.ident[0]=='.') && Drives.WildFileCmp(deident, pattern.value)
                && (~attr.value & findAttr & (Dos_system.DOS_ATTR_DIRECTORY | Dos_system.DOS_ATTR_HIDDEN | Dos_system.DOS_ATTR_SYSTEM))==0) {

                /* file is okay, setup everything to be copied in DTA Block */
                String findName = deident.toUpperCase();
                /*Bit32u*/long findSize = de.dataLengthL;
                /*Bit16u*/int findDate = Dos.DOS_PackDate(1900 + de.dateYear, de.dateMonth, de.dateDay);
                /*Bit16u*/int findTime = Dos.DOS_PackTime(de.timeHour, de.timeMin, de.timeSec);
                dta.SetResult(findName, findSize, findDate, findTime, findAttr);
                return true;
            }
        }
        // after searching the directory, free the iterator
        FreeDirIterator(dirIterator);

        Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
        return false;
    }

    public boolean Rename(String oldname,String newname) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
        return false;
    }

    public boolean GetFileAttr(String name,/*Bit16u*/IntRef attr) {
        attr.value = 0;
        isoDirEntry de = new isoDirEntry();
        boolean success = lookup(de, name);
        if (success) {
            attr.value = Dos_system.DOS_ATTR_ARCHIVE | Dos_system.DOS_ATTR_READ_ONLY;
            if (IS_HIDDEN(de.fileFlags)) attr.value |= Dos_system.DOS_ATTR_HIDDEN;
            if (IS_DIR(de.fileFlags)) attr.value |= Dos_system.DOS_ATTR_DIRECTORY;
        }
        return success;
    }

    public boolean AllocationInfo(/*Bit16u*/IntRef bytes_sector,/*Bit8u*/ShortRef sectors_cluster,/*Bit16u*/IntRef total_clusters,/*Bit16u*/IntRef free_clusters) {
        bytes_sector.value = 2048;
        sectors_cluster.value = 1; // cluster size for cdroms ?
        total_clusters.value = 60000;
        free_clusters.value = 0;
        return true;
    }

    public boolean FileExists(String name) {
        isoDirEntry de=new isoDirEntry();
        return (lookup(de, name) && !IS_DIR(de.fileFlags));
    }

    public boolean FileStat(String name, FileStat_Block stat_block) {
        isoDirEntry de=new isoDirEntry();
        boolean success = lookup(de, name);

        if (success) {
            stat_block.date = Dos.DOS_PackDate(1900 + de.dateYear, de.dateMonth, de.dateDay);
            stat_block.time = Dos.DOS_PackTime(de.timeHour, de.timeMin, de.timeSec);
            stat_block.size = de.dataLengthL;
            stat_block.attr = Dos_system.DOS_ATTR_ARCHIVE | Dos_system.DOS_ATTR_READ_ONLY;
            if (IS_DIR(de.fileFlags)) stat_block.attr |= Dos_system.DOS_ATTR_DIRECTORY;
        }

        return success;
    }

    public /*Bit8u*/short GetMediaByte() {
        return mediaid;
    }

    public boolean isRemote() {
        return true;
    }

    public boolean isRemovable() {
        return true;
    }

    public /*Bits*/int UnMount() {
        if(DosMSCDEX.MSCDEX_RemoveDrive(driveLetter)!=0) {
            return 0;
        }
        return 2;
    }

    private int GetDirIterator(isoDirEntry de) {
        int dirIterator = nextFreeDirIterator;

        // get start and end sector of the directory entry (pad end sector if necessary)
        dirIterators[dirIterator].currentSector = de.extentLocationL;
        dirIterators[dirIterator].endSector =
            de.extentLocationL + de.dataLengthL / ISO_FRAMESIZE - 1;
        if (de.dataLengthL % ISO_FRAMESIZE != 0)
            dirIterators[dirIterator].endSector++;

        // reset position and mark as valid
        dirIterators[dirIterator].pos = 0;
        dirIterators[dirIterator].valid = true;

        // advance to next directory iterator (wrap around if necessary)
        nextFreeDirIterator = (nextFreeDirIterator + 1) % DOS_Drive_Cache.MAX_OPENDIRS;

        return dirIterator;
    }

    private boolean GetNextDirEntry(int dirIteratorHandle, isoDirEntry de) {
        boolean result = false;
        /*Bit8u*/byte[][] buffer = new byte[1][];
        DirIterator dirIterator = dirIterators[dirIteratorHandle];

        // check if the directory entry is valid
        if (dirIterator.valid && ReadCachedSector(buffer, dirIterator.currentSector)) {
            // check if the next sector has to be read
            if ((dirIterator.pos >= ISO_FRAMESIZE)
             || (buffer[0][(int)dirIterator.pos] == 0)
             || (dirIterator.pos + buffer[0][(int)dirIterator.pos] > ISO_FRAMESIZE)) {

                // check if there is another sector available
                if (dirIterator.currentSector < dirIterator.endSector) {
                    dirIterator.pos = 0;
                    dirIterator.currentSector++;
                    if (!ReadCachedSector(buffer, dirIterator.currentSector)) {
                        return false;
                    }
                } else {
                    return false;
                }
             }
             // read sector and advance sector pointer
             int length = readDirEntry(de, buffer[0], (int)dirIterator.pos);
             result = length >= 0;
             dirIterator.pos += length;
        }
        return result;
    }

    private void FreeDirIterator(int dirIterator) {
        dirIterators[dirIterator].valid = false;

        // if this was the last aquired iterator decrement nextFreeIterator
        if ((dirIterator + 1) % DOS_Drive_Cache.MAX_OPENDIRS == nextFreeDirIterator) {
            if (nextFreeDirIterator>0) {
                nextFreeDirIterator--;
            } else {
                nextFreeDirIterator = DOS_Drive_Cache.MAX_OPENDIRS-1;
            }
        }
    }

    private boolean ReadCachedSector(/*Bit8u*/byte[][] buffer, /*Bit32u*/long sector) {
        // get hash table entry
        int pos = (int)sector % ISO_MAX_HASH_TABLE_SIZE;
        SectorHashEntry he = sectorHashEntries[pos];

        // check if the entry is valid and contains the correct sector
        if (!he.valid || he.sector != sector) {
            if (!CDROM_Interface_Image.images[subUnit].ReadSector(he.data, 0, false, (int)sector)) {
                return false;
            }
            he.valid = true;
            he.sector = sector;
        }

        buffer[0] = he.data;
        return true;
    }

    private boolean readSector(/*Bit8u*/byte[] buffer, /*Bit32u*/int sector) {
        return CDROM_Interface_Image.images[subUnit].ReadSector(buffer, 0, false, sector);
    }

    private int readDirEntry(isoDirEntry de, /*Bit8u*/byte[] data, int offset) {
        // copy data into isoDirEntry struct, data[0] = length of DirEntry
    //	if (data[0] > sizeof(isoDirEntry)) return -1;//check disabled as isoDirentry is currently 258 bytes large. So it always fits
        de.load(data, offset, data[0]);

        // xa not supported
        if (de.extAttrLength != 0) return -1;
        // interleaved mode not supported
        if (de.fileUnitSize != 0 || de.interleaveGapSize != 0) return -1;

        // modify file identifier for use with dosbox
        if ((de.length < 33 + de.fileIdentLength)) return -1;
        if (IS_DIR(de.fileFlags)) {
            if (de.fileIdentLength == 1 && de.ident[0] == 0) StringHelper.strcpy(de.ident, ".");
            else if (de.fileIdentLength == 1 && de.ident[0] == 1) StringHelper.strcpy(de.ident, "..");
            else {
                if (de.fileIdentLength > 200) return -1;
                de.ident[de.fileIdentLength] = 0;
            }
        } else {
            if (de.fileIdentLength > 200) return -1;
            de.ident[de.fileIdentLength] = 0;
            // remove any file version identifiers as there are some cdroms that don't have them
            StringHelper.strreplace(de.ident, ';', (char)0);
            // if file has no extension remove the trailing dot
            int tmp = StringHelper.strlen(de.ident);
            if (tmp > 0) {
                if (de.ident[tmp - 1] == '.') de.ident[tmp - 1] = 0;
            }
        }
        int dotpos = StringHelper.toString(de.ident).indexOf('.');
        if (dotpos>=0) {
            int len = StringHelper.strlen(de.ident);
            if (len-dotpos>4) de.ident[dotpos+4]=0;
            if (dotpos>8) {
                StringHelper.strcpy(de.ident, 8, de.ident, dotpos);
            }
        } else if (StringHelper.strlen(de.ident)>8) de.ident[8]=0;
        return de.length;
    }

//    static private class isoPVD {
// 0       /*Bit8u*/short type;
// 1       /*Bit8u*/byte[] standardIdent=new byte[5];
// 6       /*Bit8u*/short version;
// 7       /*Bit8u*/short unused1;
// 8       /*Bit8u*/byte[] systemIdent=new byte[32];
// 40       /*Bit8u*/byte[] volumeIdent=new byte[32];
// 72       /*Bit8u*/byte[] unused2=new byte[8];
// 80       /*Bit32u*/long volumeSpaceSizeL;
// 84       /*Bit32u*/long volumeSpaceSizeM;
// 88       /*Bit8u*/byte[] unused3=new byte[32];
// 120       /*Bit16u*/int volumeSetSizeL;
// 122       /*Bit16u*/int volumeSetSizeM;
// 124       /*Bit16u*/int volumeSeqNumberL;
// 126       /*Bit16u*/int volumeSeqNumberM;
// 128       /*Bit16u*/int logicBlockSizeL;
// 130       /*Bit16u*/int logicBlockSizeM;
// 132       /*Bit32u*/long pathTableSizeL;
// 136       /*Bit32u*/long pathTableSizeM;
// 140       /*Bit32u*/long locationPathTableL;
// 144       /*Bit32u*/long locationOptPathTableL;
// 148       /*Bit32u*/long locationPathTableM;
// 152       /*Bit32u*/long locationOptPathTableM;
// 156       /*Bit8u*/byte[] rootEntry=new byte[34];
// 190       /*Bit32u*/int[] unused4=new int[1858];
// 7622
//        static final int size = 7622;
//
//        public void load(byte[] b) {
//            Ptr p = new Ptr(b, 0);
//        }
//    }

    boolean loadImage() {
        dataCD = false;
        byte[] b = new byte[8000];
        readSector(b, ISO_FIRST_VD);
        if (/*pvd.type*/b[0] != 1 || !StringHelper.toString(/*pvd.standardIdent*/b, 1, 5).startsWith("CD001") || /*pvd.version*/b[6] != 1) return false;
        // :TODO: double check that 156 is the right offset
        if (readDirEntry(this.rootEntry, /*pvd.rootEntry*/b, 156)>0) {
            dataCD = true;
            return true;
        }
        return false;
    }

    boolean lookup(isoDirEntry de, String path) {
        if (!dataCD) return false;
        de.copy(this.rootEntry);
        if (path.length()==0) return true;

        String[] isoPath = StringHelper.split(StringHelper.replace(path, "\\", "/"), "/");

        // iterate over all path elements (name), and search each of them in the current de
        for(int i=0;i<isoPath.length;i++) {
            String name = isoPath[i];
            boolean found = false;
            // current entry must be a directory, abort otherwise
            if (IS_DIR(de.fileFlags)) {

                // remove the trailing dot if present
                int nameLength = name.length();
                if (nameLength > 0) {
                    if (name.charAt(nameLength - 1) == '.') name = name.substring(0, nameLength - 1);
                }

                // look for the current path element
                int dirIterator = GetDirIterator(de);
                while (!found && GetNextDirEntry(dirIterator, de)) {
                    if (!IS_ASSOC(de.fileFlags) && name.compareToIgnoreCase(StringHelper.toString(de.ident)) == 0) {
                        found = true;
                    }
                }
                FreeDirIterator(dirIterator);
            }
            if (!found) return false;
        }
        return true;
    }

}
