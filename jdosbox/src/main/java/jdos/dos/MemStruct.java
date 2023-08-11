package jdos.dos;

import jdos.hardware.Memory;

public class MemStruct {
    public /*Bitu*/int GetIt(/*Bitu*/int size, /*PhysPt*/int addr) {
		switch (size) {
		case 1:return Memory.mem_readb(pt+addr);
		case 2:return Memory.mem_readw(pt+addr);
		case 4:return Memory.mem_readd(pt + addr);
		}
		return 0;
	}
	public void SaveIt(/*Bitu*/int size,/*PhysPt*/int addr,/*Bitu*/int val) {
		switch (size) {
		case 1:Memory.mem_writeb(pt+addr,val);break;
		case 2:Memory.mem_writew(pt+addr,(short)val);break;
		case 4:Memory.mem_writed(pt+addr,val);break;
		}
	}
	void SetPt(/*Bit16u*/int seg) { pt=Memory.PhysMake(seg,0);}
	void SetPt(/*Bit16u*/int seg,/*Bit16u*/int off) { pt=Memory.PhysMake(seg,off);}
	void SetRealPt(/*RealPt*/int addr) { pt=Memory.Real2Phys(addr);}

	protected /*PhysPt*/int pt;
}
