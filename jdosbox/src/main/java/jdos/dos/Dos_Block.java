package jdos.dos;

public class Dos_Block {
    public Dos_Date date = new Dos_Date();
	public Dos_Version version = new Dos_Version();
	public /*Bit16u*/int firstMCB;
	public /*Bit16u*/int errorcode;
	public /*Bit16u*/int psp(){return new Dos_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).GetPSP();}
	public void psp(/*Bit16u*/int _seg){ new Dos_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).SetPSP(_seg);}
	public /*Bit16u*/int env;
	public /*RealPt*/int cpmentry;
	public /*RealPt*/int dta(){return new Dos_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).GetDTA();}
	public void dta(/*RealPt*/int _dta){new Dos_SDA(Dos.DOS_SDA_SEG,Dos.DOS_SDA_OFS).SetDTA(_dta);}
	public /*Bit8u*/short return_code,return_mode;

	public /*Bit8u*/byte current_drive;
	boolean verify;
	boolean breakcheck;
	public boolean echo;          // if set to true dev_con::read will echo input
	public static class Tables  {
		/*RealPt*/int mediaid;
		public /*RealPt*/int tempdta;
		/*RealPt*/int tempdta_fcbdelete;
		/*RealPt*/int dbcs;
		/*RealPt*/int filenamechar;
		/*RealPt*/int collatingseq;
		/*RealPt*/int upcase;
		/*Bit8u*/byte[] country;//Will be copied to dos memory. resides in real mem
		/*Bit16u*/int dpb; //Fake Disk parameter system using only the first entry so the drive letter matches
	}
    public Tables tables = new Tables();
	/*Bit16u*/int loaded_codepage=437;
}
