package jdos.dos;

import jdos.util.IntRef;
import jdos.util.ShortRef;

public abstract class Dos_Drive {
    public abstract DOS_File FileOpen(String name,/*Bit32u*/int flags);
    public abstract DOS_File FileCreate(String name,/*Bit16u*/int attributes);
    public abstract boolean FileUnlink(String _name);
    public abstract boolean RemoveDir(String _dir);
    public abstract boolean MakeDir(String _dir);
    public abstract boolean TestDir(String _dir);
    public abstract boolean FindFirst(String _dir,Dos_DTA dta,boolean fcb_findfirst/*=false*/);
    public abstract boolean FindNext(Dos_DTA dta);
    public abstract boolean GetFileAttr(String name,/*Bit16u*/IntRef attr);
    public abstract boolean Rename(String oldname,String newname);
    public abstract boolean AllocationInfo(/*Bit16u*/IntRef _bytes_sector,/*Bit8u*/ShortRef _sectors_cluster,/*Bit16u*/IntRef _total_clusters,/*Bit16u*/IntRef _free_clusters);
    public abstract boolean FileExists(String name);
    public abstract boolean FileStat(String name, FileStat_Block stat_block);
    public abstract /*Bit8u*/short GetMediaByte();
    public void SetDir(String path) { curdir=path; }
    public void EmptyCache() { dirCache.EmptyCache(); }
    public abstract boolean isRemote();
    public abstract boolean isRemovable();
    public abstract /*Bits*/int UnMount();

    public String GetInfo() {return info;}

    protected String curdir="";
    protected String info="";
    /* Can be overridden for example in iso images */
    public String GetLabel(){return dirCache.GetLabel();}

    public DOS_Drive_Cache dirCache = new DOS_Drive_Cache();

    // disk cycling functionality (request resources)
    public void Activate() {}
}
