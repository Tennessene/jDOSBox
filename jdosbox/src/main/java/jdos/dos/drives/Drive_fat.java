package jdos.dos.drives;

import jdos.dos.*;
import jdos.hardware.Memory;
import jdos.ints.Bios_disk;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.*;

import java.io.File;

public class Drive_fat extends Dos_Drive {
    static private final int IMGTYPE_FLOPPY = 0;
    static private final int IMGTYPE_ISO = 1;
    static private final int IMGTYPE_HDD = 2;

    static private final int FAT12 = 0;
    static private final int FAT16 = 1;
    static private final int FAT32 = 2;

    private /*Bit8u*/ byte[] fatSectBuffer = new byte[1024];
    private Ptr pfatSectBuffer = new Ptr(fatSectBuffer, 0);
    /*Bit32u*/ long curFatSect;

    static public class fatFile extends DOS_File {
        public fatFile(String name, /*Bit32u*/long startCluster, /*Bit32u*/long fileLen, Drive_fat useDrive) {
            /*Bit32u*/
            LongRef seekto = new LongRef(0);
            firstCluster = startCluster;
            myDrive = useDrive;
            filelength = fileLen;
            open = true;
            loadedSector = false;
            curSectOff = 0;
            seekpos = 0;
            this.name = name;
            if (filelength > 0) {
                Seek(seekto, Dos_files.DOS_SEEK_SET);
                myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);
                loadedSector = true;
            }
        }

        public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
            if ((flags & 0xf) == Dos_files.OPEN_WRITE) {    // check if file opened in write-only mode
                Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
                return false;
            }
            /*Bit16u*/
            int sizedec, sizecount;
            if (seekpos >= filelength) {
                size.value = 0;
                return true;
            }

            if (!loadedSector) {
                currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                if (currentSector == 0) {
                    /* EOC reached before EOF */
                    size.value = 0;
                    loadedSector = false;
                    return true;
                }
                curSectOff = 0;
                myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);
                loadedSector = true;
            }

            sizedec = size.value;
            sizecount = 0;
            while (sizedec != 0) {
                if (seekpos >= filelength) {
                    size.value = sizecount;
                    return true;
                }
                data[sizecount++] = sectorBuffer[curSectOff++];
                seekpos++;
                if (curSectOff >= myDrive.getSectorSize()) {
                    currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                    if (currentSector == 0) {
                        /* EOC reached before EOF */
                        //LOG_MSG("EOC reached before EOF, seekpos %d, filelen %d", seekpos, filelength);
                        size.value = sizecount;
                        loadedSector = false;
                        return true;
                    }
                    curSectOff = 0;
                    myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);
                    loadedSector = true;
                    //LOG_MSG("Reading absolute sector at %d for seekpos %d", currentSector, seekpos);
                }
                --sizedec;
            }
            size.value = sizecount;
            return true;
        }

        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            /* TODO: Check for read-only bit */

            if ((this.flags & 0xf) == Dos_files.OPEN_READ) {    // check if file opened in read-only mode
                Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
                return false;
            }

            DirEntry tmpentry = new DirEntry();
            /*Bit16u*/
            int sizedec, sizecount;
            sizedec = size.value;
            sizecount = 0;
            boolean finalizeWrite = false;
            while (sizedec != 0) {
                /* Increase filesize if necessary */
                if (seekpos >= filelength) {
                    if (filelength == 0) {
                        firstCluster = myDrive.getFirstFreeClust();
                        myDrive.allocateCluster(firstCluster, 0);
                        currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                        myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);
                        loadedSector = true;
                    }
                    filelength = seekpos + 1;
                    if (!loadedSector) {
                        currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                        if (currentSector == 0) {
                            /* EOC reached before EOF - try to increase file allocation */
                            myDrive.appendCluster(firstCluster);
                            /* Try getting sector again */
                            currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                            if (currentSector == 0) {
                                /* No can do. lets give up and go home.  We must be out of room */
                                finalizeWrite = true;
                                break;
                            }
                        }
                        curSectOff = 0;
                        myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);

                        loadedSector = true;
                    }
                }
                sectorBuffer[curSectOff++] = data[sizecount++];
                seekpos++;
                if (curSectOff >= myDrive.getSectorSize()) {
                    if (loadedSector) myDrive.loadedDisk.Write_AbsoluteSector(currentSector, sectorBuffer, 0);

                    currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                    if (currentSector == 0) {
                        /* EOC reached before EOF - try to increase file allocation */
                        myDrive.appendCluster(firstCluster);
                        /* Try getting sector again */
                        currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
                        if (currentSector == 0) {
                            /* No can do. lets give up and go home.  We must be out of room */
                            loadedSector = false;
                            finalizeWrite = true;
                            break;
                        }
                    }
                    curSectOff = 0;
                    myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);

                    loadedSector = true;
                }
                --sizedec;
            }
            if (!finalizeWrite)
                if (curSectOff > 0 && loadedSector)
                    myDrive.loadedDisk.Write_AbsoluteSector(currentSector, sectorBuffer, 0);

            //finalizeWrite:
            myDrive.directoryBrowse(dirCluster, tmpentry, (int) dirIndex);
            tmpentry.entrysize = filelength;
            tmpentry.loFirstClust = (/*Bit16u*/int) firstCluster;
            myDrive.directoryChange(dirCluster, tmpentry, (int) dirIndex);

            size.value = sizecount;
            return true;
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
                    seekto = (/*Bit32s*/int) pos.value + (/*Bit32s*/int) seekpos;
                    break;
                case Dos_files.DOS_SEEK_END:
                    seekto = (/*Bit32s*/int) filelength + (/*Bit32s*/int) pos.value;
                    break;
            }
            //	LOG_MSG("Seek to %d with type %d (absolute value %d)", *pos, type, seekto);

            if ((/*Bit32u*/long) seekto > filelength) seekto = (/*Bit32s*/int) filelength;
            if (seekto < 0) seekto = 0;
            seekpos = (/*Bit32u*/long) seekto;
            currentSector = myDrive.getAbsoluteSectFromBytePos(firstCluster, seekpos);
            if (currentSector == 0) {
                /* not within file size, thus no sector is available */
                loadedSector = false;
            } else {
                loadedSector = true;
                curSectOff = (int) (seekpos % myDrive.getSectorSize());
                myDrive.loadedDisk.Read_AbsoluteSector(currentSector, sectorBuffer, 0);
            }
            pos.value = seekpos;
            return true;
        }

        public boolean Close() {
            /* Flush buffer */
            if (loadedSector) myDrive.loadedDisk.Write_AbsoluteSector(currentSector, sectorBuffer, 0);

            return false;
        }

        public /*Bit16u*/int GetInformation() {
            return 0;
        }

        public boolean UpdateDateTimeFromHost() {
            return true;
        }

        public /*Bit32u*/ long firstCluster;
        public /*Bit32u*/ long seekpos;
        public /*Bit32u*/ long filelength;
        public /*Bit32u*/ long currentSector;
        public /*Bit32u*/ int curSectOff;
        public /*Bit8u*/ byte[] sectorBuffer = new byte[512];
        /* Record of where in the directory structure this file is located */
        public /*Bit32u*/ long dirCluster;
        public /*Bit32u*/ long dirIndex;

        public boolean loadedSector;
        public Drive_fat myDrive;

        private final static int NONE = 0;
        private final static int READ = 1;
        private final static int WRITE = 2;
        private int last_action;
        private /*Bit16u*/ int info;
    }


    /* IN - char * filename: Name in regular filename format, e.g. bob.txt */
    /* OUT - char * filearray: Name in DOS directory format, eleven char, e.g. bob     txt */
    static void convToDirFile(String filename, byte[] filearray) {
        /*Bit32u*/
        int charidx = 0;
        /*Bit32u*/
        int flen, i;
        flen = filename.length();
        java.util.Arrays.fill(filearray, (byte) 32);
        for (i = 0; i < flen; i++) {
            if (charidx >= 11) break;
            if (filename.charAt(i) != '.') {
                filearray[charidx] = (byte) filename.charAt(i);
                charidx++;
            } else {
                charidx = 8;
            }
        }
    }

    String[] srchInfo = new String[DOS_Drive_Cache.MAX_OPENDIRS];

    static private class Allocation {
        /*Bit16u*/ int bytes_sector;
        /*Bit8u*/ short sectors_cluster;
        /*Bit16u*/ int total_clusters;
        /*Bit16u*/ int free_clusters;
        /*Bit8u*/ short mediaid;
    }

    private Allocation allocation = new Allocation();

    private Bootstrap bootbuffer = new Bootstrap();
    private /*Bit8u*/ short fattype;
    private /*Bit32u*/ long CountOfClusters;
    private /*Bit32u*/ long partSectOff;
    private /*Bit32u*/ long firstDataSector;
    private /*Bit32u*/ long firstRootDirSect;

    private /*Bit32u*/ long cwdDirCluster;
    private /*Bit32u*/ long dirPosition; /* Position in directory search */

    public Bios_disk.imageDisk loadedDisk;
    public boolean created_successfully;

    private /*Bit32u*/long getClustFirstSect(/*Bit32u*/long clustNum) {
        return ((clustNum - 2) * bootbuffer.sectorspercluster) + firstDataSector;
    }

    private /*Bit32u*/long getClusterValue(/*Bit32u*/long clustNum) {
        /*Bit32u*/
        long fatoffset = 0;
        /*Bit32u*/
        long fatsectnum;
        /*Bit32u*/
        long fatentoff;
        /*Bit32u*/
        long clustValue = 0;

        switch (fattype) {
            case FAT12:
                fatoffset = clustNum + (clustNum / 2);
                break;
            case FAT16:
                fatoffset = clustNum * 2;
                break;
            case FAT32:
                fatoffset = clustNum * 4;
                break;
        }
        fatsectnum = bootbuffer.reservedsectors + (fatoffset / bootbuffer.bytespersector) + partSectOff;
        fatentoff = fatoffset % bootbuffer.bytespersector;

        if (curFatSect != fatsectnum) {
            /* Load two sectors at once for FAT12 */
            loadedDisk.Read_AbsoluteSector(fatsectnum, fatSectBuffer, 0);
            if (fattype == FAT12)
                loadedDisk.Read_AbsoluteSector(fatsectnum + 1, fatSectBuffer, 512);
            curFatSect = fatsectnum;
        }

        switch (fattype) {
            case FAT12:
                clustValue = pfatSectBuffer.readw((int) fatentoff);
                if ((clustNum & 0x1) != 0) {
                    clustValue >>= 4;
                } else {
                    clustValue &= 0xfff;
                }
                break;
            case FAT16:
                clustValue = pfatSectBuffer.readw((int) fatentoff);
                break;
            case FAT32:
                clustValue = pfatSectBuffer.readd((int) fatentoff);
                break;
        }

        return clustValue;
    }

    private void setClusterValue(/*Bit32u*/long clustNum, /*Bit32u*/long clustValue) {
        /*Bit32u*/
        long fatoffset = 0;
        /*Bit32u*/
        long fatsectnum;
        /*Bit32u*/
        long fatentoff;

        switch (fattype) {
            case FAT12:
                fatoffset = clustNum + (clustNum / 2);
                break;
            case FAT16:
                fatoffset = clustNum * 2;
                break;
            case FAT32:
                fatoffset = clustNum * 4;
                break;
        }
        fatsectnum = bootbuffer.reservedsectors + (fatoffset / bootbuffer.bytespersector) + partSectOff;
        fatentoff = fatoffset % bootbuffer.bytespersector;

        if (curFatSect != fatsectnum) {
            /* Load two sectors at once for FAT12 */
            loadedDisk.Read_AbsoluteSector(fatsectnum, fatSectBuffer, 0);
            if (fattype == FAT12)
                loadedDisk.Read_AbsoluteSector(fatsectnum + 1, fatSectBuffer, 512);
            curFatSect = fatsectnum;
        }

        switch (fattype) {
            case FAT12: {
                /*Bit16u*/
                int tmpValue = pfatSectBuffer.readw((int) fatentoff);
                if ((clustNum & 0x1) != 0) {
                    clustValue &= 0xfff;
                    clustValue <<= 4;
                    tmpValue &= 0xf;
                    tmpValue |= (/*Bit16u*/int) clustValue;

                } else {
                    clustValue &= 0xfff;
                    tmpValue &= 0xf000;
                    tmpValue |= (/*Bit16u*/int) clustValue;
                }
                pfatSectBuffer.writew((int) fatentoff, tmpValue);
                break;
            }
            case FAT16:
                pfatSectBuffer.writew((int) fatentoff, (int) clustValue);
                break;
            case FAT32:
                pfatSectBuffer.writed((int) fatentoff, clustValue);
                break;
        }
        for (int fc = 0; fc < bootbuffer.fatcopies; fc++) {
            loadedDisk.Write_AbsoluteSector(fatsectnum + (fc * bootbuffer.sectorsperfat), fatSectBuffer, 0);
            if (fattype == FAT12) {
                if (fatentoff >= 511)
                    loadedDisk.Write_AbsoluteSector(fatsectnum + 1 + (fc * bootbuffer.sectorsperfat), fatSectBuffer, 512);
            }
        }
    }

    private String getEntryName(String fullname) {
        return new File(fullname).getName();
    }

    private boolean getFileDirEntry(String filename, DirEntry useEntry, /*Bit32u*/LongRef dirClust, /*Bit32u*/LongRef subEntry) {
        String[] parts = StringHelper.split(filename, "\\");
        /*Bit32u*/
        long currentClust = 0;

        DirEntry foundEntry = new DirEntry();
        String findFile = filename;

        /* Skip if testing in root directory */
        if (!filename.endsWith("\\")) {
            //LOG_MSG("Testing for filename %s", filename);
            int index = 0;
            while (parts.length > index) {
                Bios_disk.imgDTA.SetupSearch(0, Dos_system.DOS_ATTR_DIRECTORY, parts[index]);
                Bios_disk.imgDTA.SetDirID(0);

                findFile = parts[index];
                if (!FindNextInternal(currentClust, Bios_disk.imgDTA, foundEntry, parts[index])) break;
                else {
                    //Found something. See if it's a directory (findfirst always finds regular files)
                    StringRef find_name = new StringRef();/*Bit16u*/
                    IntRef find_date = new IntRef(0), find_time = new IntRef(0);/*Bit32u*/
                    LongRef find_size = new LongRef(0);/*Bit8u*/
                    ShortRef find_attr = new ShortRef();
                    Bios_disk.imgDTA.GetResult(find_name, find_size, find_date, find_time, find_attr);
                    if ((find_attr.value & Dos_system.DOS_ATTR_DIRECTORY) == 0) break;
                }

                currentClust = foundEntry.loFirstClust;
                index++;
            }
        } else {
            /* Set to root directory */
        }

        /* Search found directory for our file */
        Bios_disk.imgDTA.SetupSearch(0, 0x7, findFile);
        Bios_disk.imgDTA.SetDirID(0);
        if (!FindNextInternal(currentClust, Bios_disk.imgDTA, foundEntry, findFile)) return false;

        useEntry.copy(foundEntry);
        dirClust.value = (/*Bit32u*/long) currentClust;
        subEntry.value = ((/*Bit32u*/long) Bios_disk.imgDTA.GetDirID() - 1);
        return true;
    }

    private boolean getDirClustNum(String dir, /*Bit32u*/LongRef clustNum, boolean parDir) {
        /* Skip if testing for root directory */
        if (!dir.endsWith("\\")) {
            String[] parts = StringHelper.split(dir, "\\");
            /*Bit32u*/
            long currentClust = 0;
            DirEntry foundEntry = new DirEntry();
            //LOG_MSG("Testing for dir %s", dir);
            int index = 0;
            while (parts.length > index) {
                Bios_disk.imgDTA.SetupSearch(0, Dos_system.DOS_ATTR_DIRECTORY, parts[index]);
                Bios_disk.imgDTA.SetDirID(0);
                if (parDir && index + 1 >= parts.length) break;

                if (!FindNextInternal(currentClust, Bios_disk.imgDTA, foundEntry, null)) {
                    return false;
                } else {
                    StringRef find_name = new StringRef();/*Bit16u*/
                    IntRef find_date = new IntRef(0), find_time = new IntRef(0);/*Bit32u*/
                    LongRef find_size = new LongRef(0);/*Bit8u*/
                    ShortRef find_attr = new ShortRef();
                    Bios_disk.imgDTA.GetResult(find_name, find_size, find_date, find_time, find_attr);
                    if ((find_attr.value & Dos_system.DOS_ATTR_DIRECTORY) == 0) return false;
                }
                currentClust = foundEntry.loFirstClust;
                index++;
            }
            clustNum.value = currentClust;
        } else {
            /* Set to root directory */
            clustNum.value = 0;
        }
        return true;
    }

    public /*Bit32u*/long getSectorSize() {
        return bootbuffer.bytespersector;
    }

    public /*Bit32u*/long getAbsoluteSectFromBytePos(/*Bit32u*/long startClustNum, /*Bit32u*/long bytePos) {
        return getAbsoluteSectFromChain(startClustNum, bytePos / bootbuffer.bytespersector);
    }

    public /*Bit32u*/long getAbsoluteSectFromChain(/*Bit32u*/long startClustNum, /*Bit32u*/long logicalSector) {
        /*Bit32s*/
        long skipClust = logicalSector / bootbuffer.sectorspercluster;
        /*Bit32u*/
        long sectClust = logicalSector % bootbuffer.sectorspercluster;

        /*Bit32u*/
        long currentClust = startClustNum;
        /*Bit32u*/
        long testvalue;

        while (skipClust != 0) {
            boolean isEOF = false;
            testvalue = getClusterValue(currentClust);
            switch (fattype) {
                case FAT12:
                    if (testvalue >= 0xff8) isEOF = true;
                    break;
                case FAT16:
                    if (testvalue >= 0xfff8) isEOF = true;
                    break;
                case FAT32:
                    if (testvalue >= 0xfffffff8l) isEOF = true;
                    break;
            }
            if ((isEOF) && (skipClust >= 1)) {
                //LOG_MSG("End of cluster chain reached before end of logical sector seek!");
                return 0;
            }
            currentClust = testvalue;
            --skipClust;
        }

        return (getClustFirstSect(currentClust) + sectClust);
    }

    public void deleteClustChain(/*Bit32u*/long startCluster) {
        /*Bit32u*/
        long testvalue;
        /*Bit32u*/
        long currentClust = startCluster;
        boolean isEOF = false;
        while (!isEOF) {
            testvalue = getClusterValue(currentClust);
            if (testvalue == 0) {
                /* What the crap?  Cluster is already empty - BAIL! */
                break;
            }
            /* Mark cluster as empty */
            setClusterValue(currentClust, 0);
            switch (fattype) {
                case FAT12:
                    if (testvalue >= 0xff8) isEOF = true;
                    break;
                case FAT16:
                    if (testvalue >= 0xfff8) isEOF = true;
                    break;
                case FAT32:
                    if (testvalue >= 0xfffffff8l) isEOF = true;
                    break;
            }
            if (isEOF) break;
            currentClust = testvalue;
        }
    }

    public /*Bit32u*/long appendCluster(/*Bit32u*/long startCluster) {
        /*Bit32u*/
        long testvalue;
        /*Bit32u*/
        long currentClust = startCluster;
        boolean isEOF = false;

        while (!isEOF) {
            testvalue = getClusterValue(currentClust);
            switch (fattype) {
                case FAT12:
                    if (testvalue >= 0xff8) isEOF = true;
                    break;
                case FAT16:
                    if (testvalue >= 0xfff8) isEOF = true;
                    break;
                case FAT32:
                    if (testvalue >= 0xfffffff8l) isEOF = true;
                    break;
            }
            if (isEOF) break;
            currentClust = testvalue;
        }

        /*Bit32u*/
        long newClust = getFirstFreeClust();
        /* Drive is full */
        if (newClust == 0) return 0;

        if (!allocateCluster(newClust, currentClust)) return 0;

        zeroOutCluster(newClust);

        return newClust;
    }

    public boolean allocateCluster(/*Bit32u*/long useCluster, /*Bit32u*/long prevCluster) {

        /* Can't allocate cluster #0 */
        if (useCluster == 0) return false;

        if (prevCluster != 0) {
            /* Refuse to allocate cluster if previous cluster value is zero (unallocated) */
            if (getClusterValue(prevCluster) == 0) return false;

            /* Point cluster to new cluster in chain */
            setClusterValue(prevCluster, useCluster);
            //LOG_MSG("Chaining cluser %d to %d", prevCluster, useCluster);
        }

        switch (fattype) {
            case FAT12:
                setClusterValue(useCluster, 0xfff);
                break;
            case FAT16:
                setClusterValue(useCluster, 0xffff);
                break;
            case FAT32:
                setClusterValue(useCluster, 0xffffffffl);
                break;
        }
        return true;
    }

    public Drive_fat(String sysFilename, /*Bit32u*/long bytesector, /*Bit32u*/long cylsector, /*Bit32u*/long headscyl, /*Bit32u*/long cylinders, /*Bit32u*/long startSector) {
        created_successfully = true;
        FileIO diskfile;
        /*Bit32u*/
        long filesize;
        PartTable mbrData = new PartTable();

        if (Bios_disk.imgDTASeg == 0) {
            Bios_disk.imgDTASeg = Dos_tables.DOS_GetMemory(2);
            Bios_disk.imgDTAPtr = Memory.RealMake(Bios_disk.imgDTASeg, 0);
            Bios_disk.imgDTA = new Dos_DTA(Bios_disk.imgDTAPtr);
        }
        try {
            diskfile = FileIOFactory.open(sysFilename, FileIOFactory.MODE_READ | FileIOFactory.MODE_WRITE);
            filesize = diskfile.length() / 1024L;
        } catch (Exception e) {
            created_successfully = false;
            return;
        }

        /* Load disk image */
        loadedDisk = new Bios_disk.imageDisk(diskfile, sysFilename, filesize, (filesize > 2880));

        byte[] d = new byte[(int) loadedDisk.sector_size];

        if (filesize > 2880) {
            /* Set user specified harddrive parameters */
            loadedDisk.Set_Geometry(headscyl, cylinders, cylsector, bytesector);

            loadedDisk.Read_Sector(0, 0, 1, d);
            mbrData.load(d);

            if (mbrData.magic1 != 0x55 || mbrData.magic2 != (byte) 0xaa)
                Log.log_msg("Possibly invalid partition table in disk image.");

            startSector = 63;
            int m;
            for (m = 0; m < 4; m++) {
                /* Pick the first available partition */
                if (mbrData.pentry[m].partSize != 0x00) {
                    Log.log_msg("Using partition " + m + " on drive; skipping " + mbrData.pentry[m].absSectStart + " sectors");
                    startSector = mbrData.pentry[m].absSectStart;
                    break;
                }
            }

            if (m == 4) Log.log_msg("No good partiton found in image.");

            partSectOff = startSector;
        } else {
            /* Floppy disks don't have partitions */
            partSectOff = 0;
        }

        loadedDisk.Read_AbsoluteSector(0 + partSectOff, d, 0);
        bootbuffer.load(d);
        if ((bootbuffer.magic1 != 0x55) || (bootbuffer.magic2 != (byte) 0xaa)) {
            /* Not a FAT filesystem */
            Log.log_msg("Loaded image has no valid magicnumbers at the end!");
        }

        if (bootbuffer.sectorsperfat == 0) {
            /* FAT32 not implemented yet */
            created_successfully = false;
            return;
        }


        /* Determine FAT format, 12, 16 or 32 */

        /* Get size of root dir in sectors */
        /* TODO: Get 32-bit total sector count if needed */
        /*Bit32u*/
        long RootDirSectors = ((bootbuffer.rootdirentries * 32) + (bootbuffer.bytespersector - 1)) / bootbuffer.bytespersector;
        /*Bit32u*/
        long DataSectors;
        if (bootbuffer.totalsectorcount != 0) {
            DataSectors = bootbuffer.totalsectorcount - (bootbuffer.reservedsectors + (bootbuffer.fatcopies * bootbuffer.sectorsperfat) + RootDirSectors);
        } else {
            DataSectors = bootbuffer.totalsecdword - (bootbuffer.reservedsectors + (bootbuffer.fatcopies * bootbuffer.sectorsperfat) + RootDirSectors);

        }
        CountOfClusters = DataSectors / bootbuffer.sectorspercluster;

        firstDataSector = (bootbuffer.reservedsectors + (bootbuffer.fatcopies * bootbuffer.sectorsperfat) + RootDirSectors) + partSectOff;
        firstRootDirSect = bootbuffer.reservedsectors + (bootbuffer.fatcopies * bootbuffer.sectorsperfat) + partSectOff;

        if (CountOfClusters < 4085) {
            /* Volume is FAT12 */
            Log.log_msg("Mounted FAT volume is FAT12 with " + CountOfClusters + " clusters");
            fattype = FAT12;
        } else if (CountOfClusters < 65525) {
            Log.log_msg("Mounted FAT volume is FAT16 with " + CountOfClusters + " clusters");
            fattype = FAT16;
        } else {
            Log.log_msg("Mounted FAT volume is FAT32 with " + CountOfClusters + " clusters");
            fattype = FAT32;
        }

        /* There is no cluster 0, this means we are in the root directory */
        cwdDirCluster = 0;

        curFatSect = 0xffffffffl;
    }

    public boolean AllocationInfo(/*Bit16u*/IntRef _bytes_sector,/*Bit8u*/ShortRef _sectors_cluster,/*Bit16u*/IntRef _total_clusters,/*Bit16u*/IntRef _free_clusters) {
        /*Bit32u*/
        LongRef hs = new LongRef(0), cy = new LongRef(0), sect = new LongRef(0), sectsize = new LongRef(0);
        /*Bit32u*/
        long countFree = 0;
        /*Bit32u*/
        long i;

        loadedDisk.Get_Geometry(hs, cy, sect, sectsize);
        _bytes_sector.value = (/*Bit16u*/int) sectsize.value;
        _sectors_cluster.value = bootbuffer.sectorspercluster;
        if (CountOfClusters < 65536) _total_clusters.value = (/*Bit16u*/int) CountOfClusters;
        else {
            // maybe some special handling needed for fat32
            _total_clusters.value = (short) 65535;
        }
        for (i = 0; i < CountOfClusters; i++) if (getClusterValue(i + 2) == 0) countFree++;
        if (countFree < 65536) _free_clusters.value = (/*Bit16u*/int) countFree;
        else {
            // maybe some special handling needed for fat32
            _free_clusters.value = 65535;
        }

        return true;
    }

    public /*Bit32u*/long getFirstFreeClust() {
        /*Bit32u*/
        long i;
        for (i = 0; i < CountOfClusters; i++) {
            if (getClusterValue(i + 2) == 0) return (i + 2);
        }

        /* No free cluster found */
        return 0;
    }

    public boolean isRemote() {
        return false;
    }

    public boolean isRemovable() {
        return false;
    }

    public /*Bits*/int UnMount() {
        return 0;
    }

    public /*Bit8u*/short GetMediaByte() {
        return loadedDisk.GetBiosType();
    }

    public DOS_File FileCreate(String name,/*Bit16u*/int attributes) {
        DirEntry fileEntry = new DirEntry();
        /*Bit32u*/
        LongRef dirClust = new LongRef(0), subEntry = new LongRef(0);
        String dirName;
        byte[] pathName = new byte[11];

        /*Bit16u*/
        int save_errorcode = Dos.dos.errorcode;

        /* Check if file already exists */
        if (getFileDirEntry(name, fileEntry, dirClust, subEntry)) {
            /* Truncate file */
            fileEntry.entrysize = 0;
            directoryChange(dirClust.value, fileEntry, (int) subEntry.value);
        } else {
            /* Can we even get the name of the file itself? */
            if ((dirName = getEntryName(name)) == null) return null;
            convToDirFile(dirName, pathName);

            /* Can we find the base directory? */
            if (!getDirClustNum(name, dirClust, true)) return null;
            fileEntry = new DirEntry();
            fileEntry.entryname = pathName;
            fileEntry.attrib = (short) (attributes & 0xff);
            addDirectoryEntry(dirClust.value, fileEntry);

            /* Check if file exists now */
            if (!getFileDirEntry(name, fileEntry, dirClust, subEntry)) return null;
        }

        /* Empty file created, now lets open it */
        /* TODO: check for read-only flag and requested write access */
        fatFile file = new fatFile(name, fileEntry.loFirstClust, fileEntry.entrysize, this);
        file.flags = Dos_files.OPEN_READWRITE;
        file.dirCluster = dirClust.value;
        file.dirIndex = subEntry.value;
        /* Maybe modTime and date should be used ? (crt matches findnext) */
        file.time = fileEntry.crtTime;
        file.date = fileEntry.crtDate;

        Dos.dos.errorcode = save_errorcode;
        return file;
    }

    public boolean FileExists(String name) {
        DirEntry fileEntry = new DirEntry();
        /*Bit32u*/
        LongRef dummy1 = new LongRef(0), dummy2 = new LongRef(0);
        if (!getFileDirEntry(name, fileEntry, dummy1, dummy2)) return false;
        return true;
    }

    public DOS_File FileOpen(String name,/*Bit32u*/int flags) {
        DirEntry fileEntry = new DirEntry();
        /*Bit32u*/
        LongRef dirClust = new LongRef(0), subEntry = new LongRef(0);
        if (!getFileDirEntry(name, fileEntry, dirClust, subEntry)) return null;
        /* TODO: check for read-only flag and requested write access */
        fatFile file = new fatFile(name, fileEntry.loFirstClust, fileEntry.entrysize, this);
        file.flags = flags;
        file.dirCluster = dirClust.value;
        file.dirIndex = subEntry.value;
        /* Maybe modTime and date should be used ? (crt matches findnext) */
        file.time = fileEntry.crtTime;
        file.date = fileEntry.crtDate;
        return file;
    }

    public boolean FileStat(String name, FileStat_Block stat_block) {
        /* TODO: Stub */
        return false;
    }

    public boolean FileUnlink(String name) {
        DirEntry fileEntry = new DirEntry();
        /*Bit32u*/
        LongRef dirClust = new LongRef(0), subEntry = new LongRef(0);

        if (!getFileDirEntry(name, fileEntry, dirClust, subEntry)) return false;

        fileEntry.entryname[0] = (byte) 0xe5;
        directoryChange(dirClust.value, fileEntry, (int) subEntry.value);

        if (fileEntry.loFirstClust != 0) deleteClustChain(fileEntry.loFirstClust);

        return true;
    }

    public boolean FindFirst(String _dir, Dos_DTA dta, boolean fcb_findfirst/*=false*/) {
        /*Bit8u*/
        ShortRef attr = new ShortRef();
        StringRef pattern = new StringRef();
        dta.GetSearchParams(attr, pattern);
        if (attr.value == Dos_system.DOS_ATTR_VOLUME) {
            if (GetLabel().length() == 0) {
                Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
                return false;
            }
            dta.SetResult(GetLabel(), 0, 0, 0, (short) Dos_system.DOS_ATTR_VOLUME);
            return true;
        }
        if ((attr.value & Dos_system.DOS_ATTR_VOLUME) != 0) //check for root dir or fcb_findfirst
            Log.log(LogTypes.LOG_DOSMISC, LogSeverities.LOG_WARN, "findfirst for volumelabel used on fatDrive. Unhandled!!!!!");
        LongRef c = new LongRef(cwdDirCluster);
        if (!getDirClustNum(_dir, c, false)) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        cwdDirCluster = c.value;
        dta.SetDirID(0);
        dta.SetDirIDCluster((/*Bit16u*/int) (cwdDirCluster & 0xffff));
        DirEntry dummyClust = new DirEntry();
        return FindNextInternal(cwdDirCluster, dta, dummyClust, null);
    }

    private boolean FindNextInternal(/*Bit32u*/long dirClustNumber, Dos_DTA dta, DirEntry foundEntry, String longName) {
        DirEntry[] sectbuf = new DirEntry[16]; /* 16 directory entries per sector */
        /*Bit32u*/
        long logentsector; /* Logical entry sector */
        /*Bit32u*/
        int entryoffset;  /* Index offset within sector */
        /*Bit32u*/
        long tmpsector;
        /*Bit8u */
        ShortRef attrs = new ShortRef();
        /*Bit16u*/
        int dirPos;
        StringRef srch_pattern = new StringRef();
        byte[] d = new byte[(int) loadedDisk.sector_size];

        dta.GetSearchParams(attrs, srch_pattern);
        dirPos = dta.GetDirID();

        for (int i = 0; i < sectbuf.length; i++) {
            sectbuf[i] = new DirEntry();
        }
        String find_name = "";
        String longFileName = "";
        boolean isLong = false;

        //nextfile:
        while (true) {
            if (!isLong) {
                longFileName = "";
            }
            logentsector = dirPos / 16;
            entryoffset = dirPos % 16;

            if (dirClustNumber == 0) {
                loadedDisk.Read_AbsoluteSector(firstRootDirSect + logentsector, d, 0);
            } else {
                tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                /* A zero sector number can't happen */
                if (tmpsector == 0) {
                    Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
                    return false;
                }
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            }
            for (int i = 0; i < sectbuf.length; i++) {
                sectbuf[i].load(d, i * DirEntry.size);
            }
            dirPos++;
            dta.SetDirID(dirPos);

            /* Deleted file entry */
            if (sectbuf[entryoffset].entryname[0] == (byte) 0xe5) continue;

            /* End of directory list */
            if (sectbuf[entryoffset].entryname[0] == 0x00) {
                Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
                return false;
            }

            find_name = StringHelper.toString(sectbuf[entryoffset].entryname, 0, 8).trim();

            if ((sectbuf[entryoffset].attrib & Dos_system.DOS_ATTR_DIRECTORY) == 0 || sectbuf[entryoffset].entryname[8] != 32) {
                find_name += ".";
                find_name += StringHelper.toString(sectbuf[entryoffset].entryname, 8, 3).trim();
            }

            if ((sectbuf[entryoffset].attrib & 0xFF) == 0xF) {
                StringBuffer tmp = new StringBuffer();
                byte[] b = sectbuf[entryoffset].entryname;
                boolean end = false;

                for (int i = 1; i < 10; i += 2) {
                    char c = (char) (b[i] | (b[i + 1] << 8));
                    if (c == 0) {
                        end = true;
                        break;
                    }
                    tmp.append(c);
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].crtTime;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].crtDate;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].accessDate;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].hiFirstClust;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].modTime;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].modDate;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) sectbuf[entryoffset].entrysize;
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                if (!end) {
                    char c = (char) (sectbuf[entryoffset].entrysize >>> 16);
                    if (c == 0) {
                        end = true;
                    } else {
                        tmp.append(c);
                    }
                }
                longFileName = tmp.toString() + longFileName;
                isLong = true;
            } else {
                isLong = false;
            }
            /* Ignore files with volume label. FindFirst should search for those. (return the first one found) */
            if ((sectbuf[entryoffset].attrib & 0x8) != 0) continue;

            /* Always find ARCHIVES even if bit is not set  Perhaps test is not the best test */
            if ((~attrs.value & sectbuf[entryoffset].attrib & (Dos_system.DOS_ATTR_DIRECTORY | Dos_system.DOS_ATTR_HIDDEN | Dos_system.DOS_ATTR_SYSTEM)) != 0)
                continue;
            if (!Drives.WildFileCmp(find_name, srch_pattern.value)) {
                if ((longFileName.length()!=0 && longName!=null && longFileName.equalsIgnoreCase(longName)))
                    break;
                continue;
            }

            break;
        }
        dta.SetResult(find_name, sectbuf[entryoffset].entrysize, sectbuf[entryoffset].crtDate, sectbuf[entryoffset].crtTime, sectbuf[entryoffset].attrib);
        foundEntry.copy(sectbuf[entryoffset]);
        return true;
    }

    public boolean FindNext(Dos_DTA dta) {
        DirEntry dummyClust = new DirEntry();
        return FindNextInternal(dta.GetDirIDCluster(), dta, dummyClust, null);
    }

    public boolean GetFileAttr(String name,/*Bit16u*/IntRef attr) {
        DirEntry fileEntry = new DirEntry();
        /*Bit32u*/
        LongRef dirClust = new LongRef(0), subEntry = new LongRef(0);
        if (!getFileDirEntry(name, fileEntry, dirClust, subEntry)) {
            String dirName;
            byte[] pathName = new byte[11];

            /* Can we even get the name of the directory itself? */
            if ((dirName = getEntryName(name)) == null) return false;
            convToDirFile(dirName, pathName);

            /* Get parent directory starting cluster */
            if (!getDirClustNum(name, dirClust, true)) return false;

            /* Find directory entry in parent directory */
            /*Bit32s*/
            int fileidx = 2;
            if (dirClust.value == 0) fileidx = 0;    // root directory
            int last_idx=0;
            while (directoryBrowse(dirClust.value, fileEntry, fileidx, last_idx)) {
                if (StringHelper.memcmp(fileEntry.entryname, pathName, 11) == 0) {
                    attr.value = fileEntry.attrib;
                    return true;
                }
                last_idx=fileidx;
                fileidx++;
            }
            return false;
        } else attr.value = fileEntry.attrib;
        return true;
    }

    public boolean directoryBrowse(/*Bit32u*/long dirClustNumber, DirEntry useEntry, /*Bit32s*/int entNum) {
        return directoryBrowse(dirClustNumber, useEntry, entNum, 0);
    }

    public boolean directoryBrowse(/*Bit32u*/long dirClustNumber, DirEntry useEntry, /*Bit32s*/int entNum, int start) {
        DirEntry[] sectbuf = new DirEntry[16];    /* 16 directory entries per sector */
        /*Bit32u*/
        long logentsector;    /* Logical entry sector */
        /*Bit32u*/
        int entryoffset = 0;    /* Index offset within sector */
        /*Bit32u*/
        long tmpsector;
        /*Bit16u*/
        if ((start<0) || (start>65535)) return false;
        /*Bit16u*/int dirPos = start;
        if (entNum<start) return false;
        entNum-=start;

        for (int i = 0; i < sectbuf.length; i++)
            sectbuf[i] = new DirEntry();
        byte[] d = new byte[(int) loadedDisk.sector_size];
        while (entNum >= 0) {

            logentsector = dirPos / 16;
            entryoffset = dirPos % 16;

            if (dirClustNumber == 0) {
                if (dirPos >= bootbuffer.rootdirentries) return false;
                tmpsector = firstRootDirSect + logentsector;
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            } else {
                tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                /* A zero sector number can't happen */
                if (tmpsector == 0) return false;
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            }
            for (int i = 0; i < sectbuf.length; i++)
                sectbuf[i].load(d, i * DirEntry.size);
            dirPos++;


            /* End of directory list */
            if (sectbuf[entryoffset].entryname[0] == 0x00) return false;
            --entNum;
        }

        useEntry.copy(sectbuf[entryoffset]);
        return true;
    }

    public boolean directoryChange(/*Bit32u*/long dirClustNumber, DirEntry useEntry, /*Bit32s*/int entNum) {
        DirEntry[] sectbuf = new DirEntry[16];    /* 16 directory entries per sector */
        /*Bit32u*/
        long logentsector;    /* Logical entry sector */
        /*Bit32u*/
        int entryoffset = 0;    /* Index offset within sector */
        /*Bit32u*/
        long tmpsector = 0;
        /*Bit16u*/
        int dirPos = 0;

        for (int i = 0; i < sectbuf.length; i++)
            sectbuf[i] = new DirEntry();
        byte[] d = new byte[(int) loadedDisk.sector_size];
        while (entNum >= 0) {

            logentsector = dirPos / 16;
            entryoffset = dirPos % 16;

            if (dirClustNumber == 0) {
                if (dirPos >= bootbuffer.rootdirentries) return false;
                tmpsector = firstRootDirSect + logentsector;
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            } else {
                tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                /* A zero sector number can't happen */
                if (tmpsector == 0) return false;
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            }
            for (int i = 0; i < sectbuf.length; i++)
                sectbuf[i].load(d, i * DirEntry.size);
            dirPos++;

            /* End of directory list */
            if (sectbuf[entryoffset].entryname[0] == 0x00) return false;
            --entNum;
        }
        if (tmpsector != 0) {
            useEntry.save(d, entryoffset * DirEntry.size);
            loadedDisk.Write_AbsoluteSector(tmpsector, d, 0);
            return true;
        } else {
            return false;
        }
    }

    private boolean addDirectoryEntry(/*Bit32u*/long dirClustNumber, DirEntry useEntry) {
        DirEntry[] sectbuf = new DirEntry[16]; /* 16 directory entries per sector */
        /*Bit32u*/
        long logentsector; /* Logical entry sector */
        /*Bit32u*/
        int entryoffset;  /* Index offset within sector */
        /*Bit32u*/
        long tmpsector;
        /*Bit16u*/
        int dirPos = 0;

        for (int i = 0; i < sectbuf.length; i++)
            sectbuf[i] = new DirEntry();
        byte[] d = new byte[(int) loadedDisk.sector_size];
        for (; ; ) {

            logentsector = dirPos / 16;
            entryoffset = dirPos % 16;

            if (dirClustNumber == 0) {
                if (dirPos >= bootbuffer.rootdirentries) return false;
                tmpsector = firstRootDirSect + logentsector;
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            } else {
                tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                /* A zero sector number can't happen - we need to allocate more room for this directory*/
                if (tmpsector == 0) {
                    /*Bit32u*/
                    long newClust;
                    newClust = appendCluster(dirClustNumber);
                    if (newClust == 0) return false;
                    /* Try again to get tmpsector */
                    tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                    if (tmpsector == 0) return false; /* Give up if still can't get more room for directory */
                }
                loadedDisk.Read_AbsoluteSector(tmpsector, d, 0);
            }
            for (int i = 0; i < sectbuf.length; i++)
                sectbuf[i].load(d, i * DirEntry.size);
            dirPos++;

            /* Deleted file entry or end of directory list */
            if ((sectbuf[entryoffset].entryname[0] == (byte) 0xe5) || (sectbuf[entryoffset].entryname[0] == 0x00)) {
                useEntry.save(d, entryoffset * DirEntry.size);
                loadedDisk.Write_AbsoluteSector(tmpsector, d, 0);
                break;
            }
        }

        return true;
    }

    private void zeroOutCluster(/*Bit32u*/long clustNumber) {
        byte[] secBuffer = new byte[512];

        int i;
        for (i = 0; i < bootbuffer.sectorspercluster; i++) {
            loadedDisk.Write_AbsoluteSector(getAbsoluteSectFromChain(clustNumber, i), secBuffer, 0);
        }
    }

    public boolean MakeDir(String dir) {
        /*Bit32u*/
        LongRef dummyClust = new LongRef(0), dirClust = new LongRef(0);
        DirEntry tmpentry = new DirEntry();
        String dirName;
        byte[] pathName = new byte[11];

        /* Can we even get the name of the directory itself? */
        if ((dirName = getEntryName(dir)) == null) return false;
        convToDirFile(dirName, pathName);

        /* Fail to make directory if already exists */
        if (getDirClustNum(dir, dummyClust, false)) return false;

        dummyClust.value = getFirstFreeClust();
        /* No more space */
        if (dummyClust.value == 0) return false;

        if (!allocateCluster(dummyClust.value, 0)) return false;

        zeroOutCluster(dummyClust.value);

        /* Can we find the base directory? */
        if (!getDirClustNum(dir, dirClust, true)) return false;

        /* Add the new directory to the base directory */
        tmpentry.entryname = pathName;
        tmpentry.loFirstClust = (/*Bit16u*/int) (dummyClust.value & 0xffff);
        tmpentry.hiFirstClust = (/*Bit16u*/int) (dummyClust.value >>> 16);
        tmpentry.attrib = Dos_system.DOS_ATTR_DIRECTORY;
        addDirectoryEntry(dirClust.value, tmpentry);

        /* Add the [.] and [..] entries to our new directory*/
        /* [.] entry */
        tmpentry = new DirEntry();
        tmpentry.entryname = ".          ".getBytes();
        tmpentry.loFirstClust = (/*Bit16u*/int) (dummyClust.value & 0xffff);
        tmpentry.hiFirstClust = (/*Bit16u*/int) (dummyClust.value >>> 16);
        tmpentry.attrib = Dos_system.DOS_ATTR_DIRECTORY;
        addDirectoryEntry(dummyClust.value, tmpentry);

        /* [..] entry */
        tmpentry = new DirEntry();
        tmpentry.entryname = "..         ".getBytes();
        tmpentry.loFirstClust = (/*Bit16u*/int) (dirClust.value & 0xffff);
        tmpentry.hiFirstClust = (/*Bit16u*/int) (dirClust.value >>> 16);
        tmpentry.attrib = Dos_system.DOS_ATTR_DIRECTORY;
        addDirectoryEntry(dummyClust.value, tmpentry);

        return true;
    }

    public boolean RemoveDir(String dir) {
        /*Bit32u*/
        LongRef dummyClust = new LongRef(0), dirClust = new LongRef(0);
        DirEntry tmpentry = new DirEntry();
        String dirName;
        byte[] pathName = new byte[11];

        /* Can we even get the name of the directory itself? */
        if ((dirName = getEntryName(dir)) == null) return false;
        convToDirFile(dirName, pathName);

        /* Get directory starting cluster */
        if (!getDirClustNum(dir, dummyClust, false)) return false;

        /* Can't remove root directory */
        if (dummyClust.value == 0) return false;

        /* Get parent directory starting cluster */
        if (!getDirClustNum(dir, dirClust, true)) return false;

        /* Check to make sure directory is empty */
        /*Bit32u*/
        long filecount = 0;
        /* Set to 2 to skip first 2 entries, [.] and [..] */
        /*Bit32s*/
        int fileidx = 2;
        while (directoryBrowse(dummyClust.value, tmpentry, fileidx)) {
            /* Check for non-deleted files */
            if (tmpentry.entryname[0] != (byte) 0xe5) filecount++;
            fileidx++;
        }

        /* Return if directory is not empty */
        if (filecount > 0) return false;

        /* Find directory entry in parent directory */
        if (dirClust.value == 0) fileidx = 0;    // root directory
        else fileidx = 2;
        boolean found = false;
        while (directoryBrowse(dirClust.value, tmpentry, fileidx)) {
            if (StringHelper.memcmp(tmpentry.entryname, pathName, 11) == 0) {
                found = true;
                tmpentry.entryname[0] = (byte) 0xe5;
                directoryChange(dirClust.value, tmpentry, fileidx);
                deleteClustChain(dummyClust.value);

                break;
            }
            fileidx++;
        }

        if (!found) return false;

        return true;
    }

    public boolean Rename(String oldname, String newname) {
        DirEntry fileEntry1 = new DirEntry();
        /*Bit32u*/
        LongRef dirClust1 = new LongRef(0), subEntry1 = new LongRef(0);
        if (!getFileDirEntry(oldname, fileEntry1, dirClust1, subEntry1)) return false;
        /* File to be renamed really exists */

        DirEntry fileEntry2 = new DirEntry();
        /*Bit32u*/
        LongRef dirClust2 = new LongRef(0), subEntry2 = new LongRef(0);

        /* Check if file already exists */
        if (!getFileDirEntry(newname, fileEntry2, dirClust2, subEntry2)) {
            /* Target doesn't exist, can rename */

            String dirName2;
            byte[] pathName2 = new byte[11];
            /* Can we even get the name of the file itself? */
            if ((dirName2 = getEntryName(newname)) == null) return false;
            convToDirFile(dirName2, pathName2);

            /* Can we find the base directory? */
            if (!getDirClustNum(newname, dirClust2, true)) return false;
            fileEntry2.copy(fileEntry1);
            fileEntry2.entryname = pathName2;
            addDirectoryEntry(dirClust2.value, fileEntry2);

            /* Check if file exists now */
            if (!getFileDirEntry(newname, fileEntry2, dirClust2, subEntry2)) return false;

            /* Remove old entry */
            fileEntry1.entryname[0] = (byte) 0xe5;
            directoryChange(dirClust1.value, fileEntry1, (int) subEntry1.value);

            return true;
        }

        /* Target already exists, fail */
        return false;
    }

    public boolean TestDir(String dir) {
        /*Bit32u*/
        LongRef dummyClust = new LongRef(0);
        return getDirClustNum(dir, dummyClust, false);
    }

}
