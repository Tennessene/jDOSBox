package jdos.dos.drives;

import jdos.util.Ptr;

public class PartTable {
    public PartTable() {
        for (int i=0;i<pentry.length;i++)
            pentry[i] = new Pentry();
    }
	public final /*Bit8u*/byte[] booter = new byte[446];
	public static class Pentry {
		/*Bit8u*/short bootflag;
		/*Bit8u*/final short[] beginchs=new short[3];
		/*Bit8u*/short parttype;
		/*Bit8u*/final short[] endchs=new short[3];
		/*Bit32u*/long absSectStart;
		/*Bit32u*/long partSize;
	}
    final Pentry[] pentry = new Pentry[4];
	/*Bit8u*/byte  magic1; /* 0x55 */
	/*Bit8u*/byte  magic2; /* 0xaa */

    public void load(byte[] d) {
        System.arraycopy(d, 0, booter, 0, booter.length);
        Ptr p = new Ptr(d, booter.length);
        for (Pentry value : pentry) {
            value.bootflag = (short) p.getInc();
            for (int j = 0; j < value.beginchs.length; j++)
                value.beginchs[j] = (short) p.getInc();
            value.parttype = (short) p.getInc();
            for (int j = 0; j < value.endchs.length; j++)
                value.endchs[j] = (short) p.getInc();
            value.absSectStart = p.readd(0);
            p.inc(4);
            value.partSize = p.readd(0);
            p.inc(4);
        }
        magic1 = (byte)p.getInc();
        magic2 = (byte)p.getInc();
    }
}
