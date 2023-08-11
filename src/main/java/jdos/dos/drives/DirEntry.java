package jdos.dos.drives;

import jdos.util.Ptr;

public class DirEntry {
	public /*Bit8u*/byte[] entryname=new byte[11];
	public /*Bit8u*/short attrib;
	public /*Bit8u*/short NTRes;
	public /*Bit8u*/short milliSecondStamp;
	public /*Bit16u*/int crtTime;
	public /*Bit16u*/int crtDate;
	public /*Bit16u*/int accessDate;
	public /*Bit16u*/int hiFirstClust;
	public /*Bit16u*/int modTime;
	public /*Bit16u*/int modDate;
	public /*Bit16u*/int loFirstClust;
	public /*Bit32u*/long entrysize;
    static public final int size = 32;

    public void load(byte[] d, int off) {
        System.arraycopy(d, off, entryname, 0, 11);
        Ptr p = new Ptr(d, off+11);
        attrib = p.readb(0); p.inc(1);
        NTRes = p.readb(0); p.inc(1);
        milliSecondStamp = p.readb(0); p.inc(1);
        crtTime = p.readw(0); p.inc(2);
        crtDate = p.readw(0); p.inc(2);
        accessDate = p.readw(0); p.inc(2);
        hiFirstClust = p.readw(0); p.inc(2);
        modTime = p.readw(0); p.inc(2);
        modDate = p.readw(0); p.inc(2);
        loFirstClust = p.readw(0); p.inc(2);
        entrysize = p.readd(0);
    }

    public void save(byte[] d, int off) {
        System.arraycopy(entryname, 0, d, off, 11);
        Ptr p = new Ptr(d, off+11);
        p.writeb(0, attrib); p.inc(1);
        p.writeb(0, NTRes); p.inc(1);
        p.writeb(0, milliSecondStamp); p.inc(1);
        p.writew(0, crtTime); p.inc(2);
        p.writew(0, crtDate); p.inc(2);
        p.writew(0, accessDate); p.inc(2);
        p.writew(0, hiFirstClust); p.inc(2);
        p.writew(0, modTime); p.inc(2);
        p.writew(0, modDate); p.inc(2);
        p.writew(0, loFirstClust); p.inc(2);
        p.writed(0, entrysize);
    }
    public void copy(DirEntry d) {
        System.arraycopy(d.entryname, 0, entryname, 0, entryname.length);
	    attrib=d.attrib;
	    NTRes=d.NTRes;
	    milliSecondStamp=d.milliSecondStamp;
	    crtTime=d.crtTime;
	    crtDate=d.crtDate;
	    accessDate=d.accessDate;
	    hiFirstClust=d.hiFirstClust;
	    modTime=d.modTime;
	    modDate=d.modDate;
	    loFirstClust=d.loFirstClust;
	    entrysize=d.entrysize;
    }
}
