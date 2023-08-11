package jdos.dos.drives;

import jdos.dos.*;
import jdos.util.IntRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

public class Drive_local_cdrom extends Drive_local {
    public Drive_local_cdrom(char driveLetter, String startdir,/*Bit16u*/int _bytes_sector,/*Bit8u*/short _sectors_cluster,/*Bit16u*/int _total_clusters,/*Bit16u*/int _free_clusters,/*Bit8u*/short _mediaid, IntRef error) {
        super(startdir,_bytes_sector,_sectors_cluster,_total_clusters,_free_clusters,_mediaid);
        // Init mscdex
        ShortRef s = new ShortRef(subUnit);
        error.value = DosMSCDEX.MSCDEX_AddDrive(driveLetter,startdir,s);
        subUnit = s.value;
        info = "CDRom "+startdir;
        this.driveLetter = driveLetter;
        // Get Volume Label
        StringRef name = new StringRef();
        if (DosMSCDEX.MSCDEX_GetVolumeName(subUnit,name)) dirCache.SetLabel(name.value,true,true);
    }

	public DOS_File FileOpen(String name,/*Bit32u*/int flags) {
        if ((flags&0xf)==Dos_files.OPEN_READWRITE) {
            flags &= ~Dos_files.OPEN_READWRITE;
        } else if ((flags&0xf)== Dos_files.OPEN_WRITE) {
            Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
            return null;
        }
        DOS_File result = super.FileOpen(name,flags);
        if (result!=null) ((localFile)result).FlagReadOnlyMedium();
        return result;
    }

	public boolean FileCreate(DOS_File file,String name,/*Bit16u*/int attributes) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
	    return false;
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

	public boolean Rename(String oldname,String newname) {
        Dos.DOS_SetError(Dos.DOSERR_ACCESS_DENIED);
	    return false;
    }

	public boolean GetFileAttr(String name,/*Bit16u*/IntRef attr) {
        boolean result = super.GetFileAttr(name,attr);
	    if (result) attr.value |= Dos_system.DOS_ATTR_READ_ONLY;
	    return result;
    }

	public boolean FindFirst(String _dir, Dos_DTA dta,boolean fcb_findfirst/*=false*/) {
        // If media has changed, reInit drivecache.
        if (DosMSCDEX.MSCDEX_HasMediaChanged(subUnit)) {
            dirCache.EmptyCache();
            // Get Volume Label
            StringRef name = new StringRef();
            if (DosMSCDEX.MSCDEX_GetVolumeName(subUnit,name)) dirCache.SetLabel(name.value,true,true);
        }
        return super.FindFirst(_dir,dta, fcb_findfirst); // :TODO: added fcb_findfirst
    }

	public void SetDir(String path) {
        // If media has changed, reInit drivecache.
        if (DosMSCDEX.MSCDEX_HasMediaChanged(subUnit)) {
            dirCache.EmptyCache();
            // Get Volume Label
            StringRef name=new StringRef();
            if (DosMSCDEX.MSCDEX_GetVolumeName(subUnit,name)) dirCache.SetLabel(name.value,true,true);
        }
        super.SetDir(path);
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

	private /*Bit8u*/short subUnit;
	private char driveLetter;
}
