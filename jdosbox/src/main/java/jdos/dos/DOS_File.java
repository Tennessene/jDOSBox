package jdos.dos;

import jdos.util.IntRef;
import jdos.util.LongRef;

public abstract class DOS_File {
    public DOS_File() {
    }
    public DOS_File(DOS_File orig) {
        flags=orig.flags;
        time=orig.time;
        date=orig.date;
        attr=orig.attr;
        refCtr=orig.refCtr;
        open=orig.open;
        hdrive=orig.hdrive;
        name=orig.name;
    }
    public abstract boolean	Read(byte[] data,/*Bit16u*/IntRef size);
    public abstract boolean	Write(byte[] data,/*Bit16u*/IntRef size);
    public abstract boolean	Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type);
    public abstract boolean	Close();
    public abstract /*Bit16u*/int GetInformation();
    public void	SetName(String _name)	{ name = _name;}
    public String GetName()				{ return name; }
    public boolean	IsOpen()					{ return open; }
    public boolean	IsName(String _name)	{ if (name==null) return false; return name.compareToIgnoreCase(_name)==0; }
    public void	AddRef()					{ refCtr++; }
    public /*Bits*/int	RemoveRef()					{ return --refCtr; }
    public boolean	UpdateDateTimeFromHost()	{ return true; }
    public void SetDrive(/*Bit8u*/short drv) { hdrive=drv;}
    public /*Bit8u*/short GetDrive() { return hdrive;}
    public /*Bit32u*/long flags;
    public /*Bit16u*/int time;
    public /*Bit16u*/int date;
    public /*Bit16u*/int attr;
    public /*Bits*/int refCtr;
    public boolean open;
    public String name;
    /* Some Device Specific Stuff */
    private /*Bit8u*/short hdrive=0xff;
}
