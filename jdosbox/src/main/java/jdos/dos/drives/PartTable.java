package jdos.dos.drives;

import jdos.util.Ptr;

public class PartTable {
    public PartTable() {
        for (int i=0;i<pentry.length;i++)
            pentry[i] = new Pentry();
    }
	public /*Bit8u*/byte[] booter = new byte[446];
	public static class Pentry {
		/*Bit8u*/short bootflag;
		/*Bit8u*/short[] beginchs=new short[3];
		/*Bit8u*/short parttype;
		/*Bit8u*/short[] endchs=new short[3];
		/*Bit32u*/long absSectStart;
		/*Bit32u*/long partSize;
	}
    Pentry[] pentry = new Pentry[4];
	/*Bit8u*/byte  magic1; /* 0x55 */
	/*Bit8u*/byte  magic2; /* 0xaa */

    public void load(byte[] d) {
        System.arraycopy(d, 0, booter, 0, booter.length);
        Ptr p = new Ptr(d, booter.length);
        for (int i=0;i<pentry.length;i++) {
            pentry[i].bootflag = (short)p.getInc();
            for (int j=0;j<pentry[i].beginchs.length;j++)
                pentry[i].beginchs[j] = (short)p.getInc();
            pentry[i].parttype = (short)p.getInc();
            for (int j=0;j<pentry[i].endchs.length;j++)
                pentry[i].endchs[j] = (short)p.getInc();
            pentry[i].absSectStart = p.readd(0); p.inc(4);
            pentry[i].partSize = p.readd(0); p.inc(4);
        }
        magic1 = (byte)p.getInc();
        magic2 = (byte)p.getInc();
    }
}
