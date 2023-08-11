package jdos.dos;

public class Dos_ParamBlock extends MemStruct {
	public Dos_ParamBlock(/*PhysPt*/int addr) {pt=addr;}
    
	public void Clear() {
        exec = new sExec();
	    overlay = new sOverlay();
    }

	public void LoadData() {
        exec.envseg=GetIt(2,0);//(Bit16u)sGet(sExec,envseg);
        exec.cmdtail=GetIt(4,2);//sGet(sExec,cmdtail);
        exec.fcb1=GetIt(4,6);//sGet(sExec,fcb1);
        exec.fcb2=GetIt(4,10);//sGet(sExec,fcb2);
        exec.initsssp=GetIt(4,14);//sGet(sExec,initsssp);
        exec.initcsip=GetIt(4,18);//sGet(sExec,initcsip);
        overlay.loadseg=GetIt(2,0);//(Bit16u)sGet(sOverlay,loadseg);
        overlay.relocation=GetIt(2,2);//(Bit16u)sGet(sOverlay,relocation);
    }

	public void SaveData() {		/* Save it as an exec block */
        SaveIt(2,0,exec.envseg);//sSave(sExec,envseg,exec.envseg);
        SaveIt(4,2,(int)exec.cmdtail);//sSave(sExec,cmdtail,exec.cmdtail);
        SaveIt(4,6,(int)exec.fcb1);//sSave(sExec,fcb1,exec.fcb1);
        SaveIt(4,10,(int)exec.fcb2);//sSave(sExec,fcb2,exec.fcb2);
        SaveIt(4,14,(int)exec.initsssp);//sSave(sExec,initsssp,exec.initsssp);
        SaveIt(4,18,(int)exec.initcsip);//sSave(sExec,initcsip,exec.initcsip);
    }

	public static class sOverlay {
		public /*Bit16u*/int loadseg;
		public /*Bit16u*/int relocation;
	}
	public static class sExec {
		public /*Bit16u*/int envseg;
		public /*RealPt*/int cmdtail;
		public /*RealPt*/int fcb1;
		public /*RealPt*/int fcb2;
		public /*RealPt*/int initsssp;
		public /*RealPt*/int initcsip;
	}

	public sExec exec = new sExec();
	sOverlay overlay = new sOverlay();
}
