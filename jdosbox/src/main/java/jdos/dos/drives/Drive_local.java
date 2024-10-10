package jdos.dos.drives;

import jdos.dos.*;
import jdos.hardware.IoHandler;
import jdos.misc.Log;
import jdos.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Drive_local extends Dos_Drive {
    static public class localFile extends DOS_File {
        public localFile(String machinePath, String name, FileIO handle) {
            fhandle=handle;
            open=true;
            UpdateDateTimeFromHost();
            attr= Dos_system.DOS_ATTR_ARCHIVE;
            last_action=Last_action.NONE;
            read_only_medium=false;
            SetName(name);
            this.machinePath = machinePath;
        }

        public String GetPath() {
            return machinePath;
        }
        public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
            if ((this.flags & 0xf) == Dos_files.OPEN_WRITE) {	// check if file opened in write-only mode
                Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
                return false;
            }
            //if (last_action==Last_action.WRITE) fseek(fhandle,ftell(fhandle),SEEK_SET);
            last_action=Last_action.READ;
            try {size.value=fhandle.read(data,0,size.value);} catch (Exception e) {}
            if (size.value==-1) size.value = 0;
            /* Fake harddrive motion. Inspector Gadget with soundblaster compatible */
            /* Same for Igor */
            /* hardrive motion => unmask irq 2. Only do it when it's masked as unmasking is realitively heavy to emulate */
            /*Bit8u*/int mask = IoHandler.IO_Read(0x21);
            if((mask & 0x4)!=0) IoHandler.IO_Write(0x21,mask&0xfb);
            return true;
        }

        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            if ((this.flags & 0xf) == Dos_files.OPEN_READ) {	// check if file opened in read-only mode
                Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
                return false;
            }
            //if (last_action==Last_action.READ) fseek(fhandle,ftell(fhandle),SEEK_SET);
            last_action=Last_action.WRITE;
            if(size.value==0){
                try {fhandle.setLength(fhandle.getFilePointer());} catch (Exception e){}
            }
            else
            {
                try {fhandle.write(data,0,size.value);} catch (Exception e) {}
            }
            return true;
        }
            
        public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
            int p = (int)(pos.value & 0xFFFFFFFFl);
            try {
                switch (type) {
                case Dos_files.DOS_SEEK_SET:
                    fhandle.seek(p);
                    break;
                case Dos_files.DOS_SEEK_CUR:
                    fhandle.seek(p+fhandle.getFilePointer());
                    break;
                case Dos_files.DOS_SEEK_END:
                    fhandle.seek(fhandle.length()+p);
                    break;
                default:
                //TODO Give some doserrorcode;
                    return false;//ERROR
                }
                pos.value=fhandle.getFilePointer();
            } catch (Exception e) {
            }
            last_action=Last_action.NONE;
            return true;
        }
            
        public boolean Close() {
            // only close if one reference left
            if (refCtr==1) {
                if(fhandle!=null) try {fhandle.close();} catch (Exception e){}
                fhandle = null;
                open = false;
            }
            return true;
        }
            
        public /*Bit16u*/int GetInformation() {
            return read_only_medium?0x40:0;
        }

        public boolean UpdateDateTimeFromHost() {
            if(!open) return false;
            long dt = fhandle.lastModified();
            time = CalendarHelper.Dos_time(dt);
            date = CalendarHelper.Dos_date(dt);
            return true;
        }

        public void Flush() {
            if (last_action==Last_action.WRITE) {
                // :TODO: not sure if flushing is necessary in Java
                // Betrayal In Antara only work with Dosbox with the dynamic
                // core.  jDosbox with normal core gives the same error as
                // Dosbox with the normal core so I was unable to test this
                // game.  The jDosbox dynamic core is not related to the
                // Dosbox dynamic core.  The jDosbox dynamic core is just
                // an instruction cache for the normal core.
                //fseek(fhandle,ftell(fhandle),SEEK_SET);
                last_action=Last_action.NONE;
            }
        }

        public void FlagReadOnlyMedium() {
            read_only_medium = true;
        }

        private FileIO fhandle;
        private boolean read_only_medium;
        private static final class Last_action{
            public static final int NONE=0;
            public static final int READ=1;
            public static final int WRITE=2;
        }
        private int last_action;
        private String machinePath;
    }

    public Drive_local(String startdir,/*Bit16u*/int _bytes_sector,/*Bit8u*/short _sectors_cluster,/*Bit16u*/int _total_clusters,/*Bit16u*/int _free_clusters,/*Bit8u*/short _mediaid) {
        for (int i=0;i<srchInfo.length;i++)
            srchInfo[i] = new SrchInfo();
        basedir=startdir;
        basedir = StringHelper.replace(basedir, "/", File.separator);
        basedir = StringHelper.replace(basedir, "\\", File.separator);
        if (!basedir.endsWith(File.separator))
            basedir+=File.separator;
        info="local directory "+startdir;
        allocation.bytes_sector=_bytes_sector;
        allocation.sectors_cluster=_sectors_cluster;
        allocation.total_clusters=_total_clusters;
        allocation.free_clusters=_free_clusters;
        allocation.mediaid=_mediaid;

        dirCache.SetBaseDir(basedir);
    }

    public DOS_File FileOpen(String name,/*Bit32u*/int flags) {
        int type;
        switch (flags&0xf) {
        case Dos_files.OPEN_READ:type=FileIOFactory.MODE_READ; break;
        case Dos_files.OPEN_WRITE:type=FileIOFactory.MODE_WRITE; break;
        case Dos_files.OPEN_READWRITE:type=FileIOFactory.MODE_READ|FileIOFactory.MODE_WRITE; break;
        case Dos_files.OPEN_READ_NO_MOD:type=FileIOFactory.MODE_READ; break; //No modification of dates. LORD4.07 uses this
        default:
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_CODE_INVALID);
            return null;
        }
        StringRef newname = new StringRef(basedir+name);
        dirCache.ExpandName(newname);

        //Flush the buffer of handles for the same file. (Betrayal in Antara)
        /*Bit8u*/int i,drive=Dos_files.DOS_DRIVES;
        localFile lfp;
        for (i=0;i<Dos_files.DOS_DRIVES;i++) {
            if (Dos_files.Drives[i]==this) {
                drive=i;
                break;
            }
        }
        for (i=0;i<Dos_files.DOS_FILES;i++) {
            if (Dos_files.Files[i]!=null && Dos_files.Files[i].IsOpen() && Dos_files.Files[i].GetDrive()==drive && Dos_files.Files[i].IsName(name)) {
                if (Dos_files.Files[i] instanceof localFile) {
                    ((localFile)Dos_files.Files[i]).Flush();
                }
            }
        }

        try {
            FileIO raf = FileIOFactory.open(newname.value, type);
            DOS_File file = new localFile(newname.value, name, raf);
            file.flags = flags;
            return file;
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found: "+newname.value);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public DOS_File FileCreate(String name,/*Bit16u*/int attributes) {
        //TODO Maybe care for attributes but not likely
        String newname=basedir+name;
        String temp_name = dirCache.GetExpandName(newname); //Can only be used in till a new drive_cache action is preformed */
        /* Test if file exists (so we need to truncate it). don't add to dirCache then */
        boolean existing_file = new File(temp_name).exists();

        try {
            FileIO hand=FileIOFactory.open(temp_name,FileIOFactory.MODE_READ|FileIOFactory.MODE_WRITE|FileIOFactory.MODE_TRUNCATE);
            if(!existing_file) dirCache.AddEntry(newname, true);
            /* Make the 16 bit device information */
            DOS_File file=new localFile(temp_name,name,hand);
            file.flags=Dos_files.OPEN_READWRITE;
            return file;
        } catch (Exception e) {
            Log.log_msg("Warning: file creation failed: "+newname);
            return null;
        }
    }

    public boolean FileUnlink(String name) {
        String newname=basedir+name;
        String fullname = dirCache.GetExpandName(newname);
        File f = new File(fullname);
        if (!f.delete()) {
            //Unlink failed for some reason try finding it.
            if(!f.exists()) return false; // File not found.

            try {
                FileInputStream fis = new FileInputStream(f);
                fis.close();
            } catch (Exception e) {
                return false; //No acces ? ERROR MESSAGE NOT SET. FIXME ?
            }

            //File exists and can technically be deleted, nevertheless it failed.
            //This means that the file is probably open by some process.
            //See if We have it open.
            boolean found_file = false;
            for(/*Bitu*/int i = 0;i < Dos_files.DOS_FILES;i++){
                if(Dos_files.Files[i]!=null && Dos_files.Files[i].IsName(name)) {
                    /*Bitu*/int max = Dos_files.DOS_FILES;
                    while(Dos_files.Files[i].IsOpen() && max--!=0) {
                        Dos_files.Files[i].Close();
                        if (Dos_files.Files[i].RemoveRef()<=0) break;
                    }
                    found_file=true;
                }
            }
            if(!found_file) return false;
            if (f.delete()) {
                dirCache.DeleteEntry(newname);
                return true;
            }
            return false;
        } else {
            dirCache.DeleteEntry(newname);
            return true;
        }
    }

    public boolean RemoveDir(String dir) {
        String newdir=basedir+dir;
        File f = new File((dirCache.GetExpandName(newdir)));
        boolean temp=(f.isDirectory() && f.delete());
        if (temp) dirCache.DeleteEntry(newdir,true);
        return temp;
    }

    public boolean MakeDir(String dir) {
        String newdir=basedir+dir;
        File f = new File(dirCache.GetExpandName(newdir));
        boolean temp = !f.exists() && f.mkdir();
        if (temp) dirCache.CacheOut(newdir,true);

        return temp;
    }

    public boolean TestDir(String dir) {
        StringRef newdir=new StringRef(basedir+dir);
        dirCache.ExpandName(newdir);
        // Skip directory test, if "\"
        File f = new File(newdir.value);
        if (!newdir.value.endsWith("\\")) {
            // It has to be a directory !
            return f.exists() && f.isDirectory();
        }
        return f.exists();
    }

    public boolean FindFirst(String dir, Dos_DTA dta,boolean fcb_findfirst/*=false*/) {
        StringRef tempDir=new StringRef(basedir+dir);

        if (allocation.mediaid==0xF0 ) {
            EmptyCache(); //rescan floppie-content on each findfirst
        }

        if (!tempDir.value.endsWith(File.separator))
            tempDir.value+=File.separator;

        /*Bit16u*/IntRef id=new IntRef(0);
        if (!dirCache.FindFirst(tempDir.value,id)) {
            Dos.DOS_SetError(Dos.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        srchInfo[id.value].srch_dir=tempDir.value;
        dta.SetDirID(id.value);

        /*Bit8u*/ShortRef sAttr=new ShortRef();
        dta.GetSearchParams(sAttr,tempDir);

        if (isRemote() && isRemovable()) {
            // cdroms behave a bit different than regular drives
            if (sAttr.value == Dos_system.DOS_ATTR_VOLUME) {
                dta.SetResult(dirCache.GetLabel(),0,0,0,(short)Dos_system.DOS_ATTR_VOLUME);
                return true;
            }
        } else {
            if (sAttr.value == Dos_system.DOS_ATTR_VOLUME) {
                if (dirCache.GetLabel().length()==0) {
    //				LOG(LOG_DOSMISC,LOG_ERROR)("DRIVELABEL REQUESTED: none present, returned  NOLABEL");
    //				dta.SetResult("NO_LABEL",0,0,0,DOS_ATTR_VOLUME);
    //				return true;
                    Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
                    return false;
                }
                dta.SetResult(dirCache.GetLabel(),0,0,0,(short)Dos_system.DOS_ATTR_VOLUME);
                return true;
            } else if ((sAttr.value & Dos_system.DOS_ATTR_VOLUME)!=0  && (dir.length() == 0) && !fcb_findfirst) {
            //should check for a valid leading directory instead of 0
            //exists==true if the volume label matches the searchmask and the path is valid
                if (Drives.WildFileCmp(dirCache.GetLabel(),tempDir.value)) {
                    dta.SetResult(dirCache.GetLabel(),0,0,0,(short)Dos_system.DOS_ATTR_VOLUME);
                    return true;
                }
            }
        }
        return FindNext(dta);
    }

    public boolean FindNext(Dos_DTA dta) {
        StringRef dir_ent=new StringRef();
        String full_name;
        String dir_entcopy;

        /*Bit8u*/ShortRef srch_attr=new ShortRef();StringRef srch_pattern=new StringRef();
        /*Bit8u*/int find_attr;

        dta.GetSearchParams(srch_attr,srch_pattern);
        /*Bit16u*/int id = dta.GetDirID();
        File f = null;

        while (true) {
            if (!dirCache.FindNext(id,dir_ent)) {
                Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
                return false;
            }
            if(!Drives.WildFileCmp(dir_ent.value,srch_pattern.value)) continue;

            full_name=srchInfo[id].srch_dir+dir_ent.value;

            //GetExpandName might indirectly destroy dir_ent (by caching in a new directory
            //and due to its design dir_ent might be lost.)
            //Copying dir_ent first
            dir_entcopy=dir_ent.value;
            f =new File(dirCache.GetExpandName(full_name));
            if (!f.exists()) {
                continue;//No symlinks and such
            }

            if (f.isDirectory()) find_attr=Dos_system.DOS_ATTR_DIRECTORY;
            else find_attr=Dos_system.DOS_ATTR_ARCHIVE;
            if ((~srch_attr.value & find_attr & (Dos_system.DOS_ATTR_DIRECTORY | Dos_system.DOS_ATTR_HIDDEN | Dos_system.DOS_ATTR_SYSTEM))!=0) continue;
            break;
        }
        /*file is okay, setup everything to be copied in DTA Block */
        String find_name = dir_entcopy.toUpperCase();
        /*Bit16u*/int find_date= CalendarHelper.Dos_date(f.lastModified()),find_time=CalendarHelper.Dos_time(f.lastModified());
        /*Bit32u*/long find_size = f.length();

        dta.SetResult(find_name,find_size,find_date,find_time,(short)find_attr);
        return true;
    }

    public boolean GetFileAttr(String name,/*Bit16u*/IntRef attr) {
        StringRef newname=new StringRef(basedir+name);
        dirCache.ExpandName(newname);

        File f = new File(newname.value);
        if (f.exists()) {
            attr.value=Dos_system.DOS_ATTR_ARCHIVE;
            if(f.isDirectory()) attr.value|=Dos_system.DOS_ATTR_DIRECTORY;
            return true;
        }
        attr.value=0;
        return false;
    }

    public boolean Rename(String oldname,String newname) {
        StringRef newold=new StringRef(basedir+oldname);
        dirCache.ExpandName(newold);

        StringRef newnew=new StringRef(basedir+newname);
        dirCache.ExpandName(newnew);

        File fold = new File(newold.value);
        File fnew = new File(newnew.value);

        boolean temp = fold.renameTo(fnew);
        if (temp) dirCache.CacheOut(newnew.value);
        return temp;
    }

    public boolean AllocationInfo(/*Bit16u*/IntRef _bytes_sector,/*Bit8u*/ShortRef _sectors_cluster,/*Bit16u*/IntRef _total_clusters,/*Bit16u*/IntRef _free_clusters) {
        _bytes_sector.value=allocation.bytes_sector;
        _sectors_cluster.value=allocation.sectors_cluster;
        _total_clusters.value=allocation.total_clusters;
        _free_clusters.value=allocation.free_clusters;
        return true;
    }

    public boolean FileExists(String name) {
        StringRef newname=new StringRef(basedir+name);
        dirCache.ExpandName(newname);
        try {
            FileIO raf = FileIOFactory.open(newname.value, FileIOFactory.MODE_READ);
            raf.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean FileStat(String name, FileStat_Block stat_block) {
        StringRef newname=new StringRef(basedir+name);
        dirCache.ExpandName(newname);
        File f = new File(newname.value);
        if(!f.exists()) return false;
        /* Convert the stat to a FileStat */
        stat_block.time=CalendarHelper.Dos_time(f.lastModified());
        stat_block.date=CalendarHelper.Dos_date(f.lastModified());
        stat_block.size=f.length();
        return true;
    }

    public /*Bit8u*/short GetMediaByte() {
        return allocation.mediaid;
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

	public FileIO GetSystemFilePtr(String name, String type) {
        StringRef newname=new StringRef(basedir+name);
        dirCache.ExpandName(newname);
        int mode = FileIOFactory.MODE_READ;
        if (type.indexOf('+')>=0)
            mode|= FileIOFactory.MODE_WRITE;
        try {
            return FileIOFactory.open(newname.value, mode);
        } catch (Exception e) {
            return null;
        }
    }

	public boolean GetSystemFilename(StringRef sysName, String dosName) {
        sysName.value=basedir+dosName;
        dirCache.ExpandName(sysName);
        return true;
    }

	public String basedir;
	private static class SrchInfo{
		String srch_dir;
	}
    SrchInfo[] srchInfo = new SrchInfo[DOS_Drive_Cache.MAX_OPENDIRS];

	private static class Allocation {
		/*Bit16u*/int bytes_sector;
		/*Bit8u*/short sectors_cluster;
		/*Bit16u*/int total_clusters;
		/*Bit16u*/int free_clusters;
		/*Bit8u*/short mediaid;
	}
    private Allocation allocation = new Allocation();
}
