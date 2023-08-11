package jdos.dos;

import jdos.hardware.Memory;
import jdos.util.StringRef;

public class Dos_MCB extends MemStruct{
	public Dos_MCB(/*Bit16u*/int seg) { SetPt(seg); }
	public void SetFileName(String _name) { Memory.MEM_BlockWrite(pt+8/*offsetof(sMCB,filename)*/,_name,8); }
	public void GetFileName(StringRef _name) { _name.value = Memory.MEM_BlockRead(pt+8/*offsetof(sMCB,filename)*/,8);}
	public void SetType(/*Bit8u*/short _type) { SaveIt(1,0,_type);} //sSave(sMCB,type,_type);}
	public void SetSize(/*Bit16u*/int _size) { SaveIt(2,3,_size);} //sSave(sMCB,size,_size);}
	public void SetPSPSeg(/*Bit16u*/int _pspseg) { SaveIt(2,1,_pspseg);} //sSave(sMCB,psp_segment,_pspseg);}
	public /*Bit8u*/short GetType() { return (short)GetIt(1,0);} //(Bit8u)sGet(sMCB,type);}
	public /*Bit16u*/int GetSize() { return GetIt(2,3);}//(Bit16u)sGet(sMCB,size);}
	public /*Bit16u*/int GetPSPSeg() { return GetIt(2,1);}//(Bit16u)sGet(sMCB,psp_segment);}

//	struct sMCB {
//0		Bit8u type;
//1		Bit16u psp_segment;
//3		Bit16u size;
//5		Bit8u unused[3];
//8		Bit8u filename[8];
//16 } GCC_ATTRIBUTE(packed);
}
