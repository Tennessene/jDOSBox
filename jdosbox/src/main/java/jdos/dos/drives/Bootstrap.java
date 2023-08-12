package jdos.dos.drives;

import jdos.util.Ptr;

public class Bootstrap {
    /*Bit8u*/byte[] nearjmp = new byte[3];
	/*Bit8u*/byte[] oemname=new byte[8];
	/*Bit16u*/int bytespersector;
	/*Bit8u*/short  sectorspercluster;
	/*Bit16u*/int reservedsectors;
	/*Bit8u*/short  fatcopies;
	/*Bit16u*/int rootdirentries;
	/*Bit16u*/int totalsectorcount;
	/*Bit8u*/short  mediadescriptor;
	/*Bit16u*/int sectorsperfat;
	/*Bit16u*/int sectorspertrack;
	/*Bit16u*/int headcount;
	/* 32-bit FAT extensions */
	/*Bit32u*/long hiddensectorcount;
	/*Bit32u*/long totalsecdword;
	/*Bit8u*/byte[]  bootcode=new byte[474];
	/*Bit8u*/byte  magic1; /* 0x55 */
	/*Bit8u*/byte  magic2; /* 0xaa */

    public void load(byte[] d) {
        System.arraycopy(d, 0, nearjmp, 0, 3);
        System.arraycopy(d, 3, oemname, 0, 8);
        Ptr p = new Ptr(d, 11);
        bytespersector = p.readw(0); p.inc(2);
        sectorspercluster = p.readb(0); p.inc(1);
	    reservedsectors = p.readw(0); p.inc(2);
	    fatcopies = p.readb(0); p.inc(1);
	    rootdirentries = p.readw(0); p.inc(2);
	    totalsectorcount = p.readw(0); p.inc(2);
	    mediadescriptor = p.readb(0); p.inc(1);
	    sectorsperfat = p.readw(0); p.inc(2);
	    sectorspertrack = p.readw(0); p.inc(2);
	    headcount = p.readw(0); p.inc(2);
    	hiddensectorcount = p.readd(0); p.inc(4);
	    totalsecdword = p.readd(0); p.inc(4);
        p.read(bootcode); p.inc(bootcode.length);
        magic1 = (byte)p.getInc();
        magic2 = (byte)p.getInc();
    }
}
