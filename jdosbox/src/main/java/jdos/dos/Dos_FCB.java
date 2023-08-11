package jdos.dos;

import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

public class Dos_FCB extends MemStruct {
    public Dos_FCB(/*Bit16u*/int seg,/*Bit16u*/int off) {
        this(seg, off, true);
    }
	public Dos_FCB(/*Bit16u*/int seg,/*Bit16u*/int off,boolean allow_extended/*=true*/) {
        SetPt(seg,off);
        real_pt=pt;
        extended=false;
        if (allow_extended) {
            if (GetIt(1,0)/*sGet(sFCB,drive)*/==0xff) {
                pt+=7;
                extended=true;
            }
        }
    }
	public void Create(boolean _extended) {
        /*Bitu*/int fill;
        if (_extended) fill=36+7;
        else fill=36;
        /*Bitu*/int i;
        for (i=0;i<fill;i++) Memory.mem_writeb(real_pt+i,0);
        pt=real_pt;
        if (_extended) {
            Memory.mem_writeb(real_pt,0xff);
            pt+=7;
            extended=true;
        } else extended=false;
    }
	public void SetName(/*Bit8u*/short _drive,String _fname,String _ext) {
        SaveIt(1,0, _drive);//sSave(sFCB,drive,_drive);
        Memory.MEM_BlockWrite(pt+1/*offsetof(sFCB,filename)*/,_fname,8);
        Memory.MEM_BlockWrite(pt+9/*offsetof(sFCB,ext)*/,_ext,3);
    }
	public void SetSizeDateTime(/*Bit32u*/long _size,/*Bit16u*/int _date,/*Bit16u*/int _time) {
        SaveIt(4, 16, (int)_size); //sSave(sFCB,filesize,_size);
	    SaveIt(2, 20, _date); //sSave(sFCB,date,_date);
	    SaveIt(2, 22, _time); //sSave(sFCB,time,_time);
    }
	public void GetSizeDateTime(/*Bit32u*/LongRef _size,/*Bit16u*/IntRef _date,/*Bit16u*/IntRef _time) {
        _size.value=GetIt(4,16);//sGet(sFCB,filesize);
        _date.value=GetIt(2, 20);//(/*Bit16u*/int)sGet(sFCB,date);
        _time.value=GetIt(2, 22);//(/*Bit16u*/int)sGet(sFCB,time);
    }
	public void GetName(StringRef fillname) {
        fillname.value=String.valueOf((char)(GetDrive()+'A'));
        fillname.value+=':';
        fillname.value+=Memory.MEM_BlockRead(pt+1/*offsetof(sFCB,filename)*/,8);
        fillname.value+='.';
        fillname.value+=Memory.MEM_BlockRead(pt+9/*offsetof(sFCB,ext)*/,3);
    }
	public void FileOpen(/*Bit8u*/short _fhandle) {
        SaveIt(1,0, GetDrive()+1); //sSave(sFCB,drive,GetDrive()+1);
        SaveIt(1,27, _fhandle);//sSave(sFCB,file_handle,_fhandle);
        SaveIt(2,12, 0); //sSave(sFCB,cur_block,0);
        SaveIt(2,14, 128); //sSave(sFCB,rec_size,128);
    //	sSave(sFCB,rndm,0); // breaks Jewels of darkness. 
        /*Bit8u*/int temp = Dos.RealHandle(_fhandle);
        /*Bit32u*/LongRef size = new LongRef(0);
        Dos_files.Files[temp].Seek(size,Dos_files.DOS_SEEK_END);
        SaveIt(4,16, (int)size.value);//sSave(sFCB,filesize,size);
        size.value = 0;
        Dos_files.Files[temp].Seek(size,Dos_files.DOS_SEEK_SET);
        SaveIt(2,22, Dos_files.Files[temp].time); //sSave(sFCB,time,Files[temp]->time);
        SaveIt(2,20, Dos_files.Files[temp].date); //sSave(sFCB,date,Files[temp]->date);
    }
	public void FileClose(/*Bit8u*/ShortRef _fhandle) {
        _fhandle.value=(/*Bit8u*/short)GetIt(1,27); //sGet(sFCB,file_handle);
	    SaveIt(1,27,0xff);//sSave(sFCB,file_handle,0xff);
    }
	public void GetRecord(/*Bit16u*/IntRef _cur_block,/*Bit8u*/ShortRef _cur_rec) {
        _cur_block.value=(/*Bit16u*/int)GetIt(2,12);//sGet(sFCB,cur_block);
	    _cur_rec.value=(/*Bit8u*/short)GetIt(1,32);//sGet(sFCB,cur_rec);
    }
	public void SetRecord(/*Bit16u*/int _cur_block,/*Bit8u*/short _cur_rec) {
        SaveIt(2,12,_cur_block); //sSave(sFCB,cur_block,_cur_block);
	    SaveIt(1,32, _cur_rec); //sSave(sFCB,cur_rec,_cur_rec);
    }
	public void GetSeqData(/*Bit8u*/ShortRef _fhandle,/*Bit16u*/IntRef _rec_size) {
        _fhandle.value=(/*Bit8u*/short)GetIt(1,27);//sGet(sFCB,file_handle);
	    _rec_size.value=(/*Bit16u*/int)GetIt(2,14);//sGet(sFCB,rec_size);
    }
	public void GetRandom(/*Bit32u*/LongRef _random) {
        _random.value=GetIt(4,33);//sGet(sFCB,rndm);
    }
	public void SetRandom(/*Bit32u*/long  _random) {
        SaveIt(4,33, (int)_random);//sSave(sFCB,rndm,_random);
    }
	public /*Bit8u*/short GetDrive() {
        /*Bit8u*/short drive=(/*Bit8u*/short)GetIt(1,0);//sGet(sFCB,drive);
        if (drive==0) return  Dos_files.DOS_GetDefaultDrive();
        else return (short)(drive-1);
    }
	public boolean Extended() {
        return extended;
    }
	public void GetAttr(/*Bit8u*/ShortRef attr) {
        if(extended) attr.value=(short)Memory.mem_readb(pt - 1);
    }
	public void SetAttr(/*Bit8u*/short attr) {
        if(extended) Memory.mem_writeb(pt - 1,attr);
    }
    void SetResultAttr(/*Bit8u*/short attr) {
	    Memory.mem_writeb(pt + 12,attr);
    }
	public boolean Valid() {
        //Very simple check for Oubliette
        if(GetIt(1,1)/*sGet(sFCB,filename[0])*/ == 0 && GetIt(1,27)/*sGet(sFCB,file_handle)*/ == 0) return false;
        return true;
    }

	private boolean extended;
	private /*PhysPt*/int real_pt;

//	struct sFCB {
//0		Bit8u drive;			/* Drive number 0=default, 1=A, etc */
//1		Bit8u filename[8];		/* Space padded name */
//9		Bit8u ext[3];			/* Space padded extension */
//12	Bit16u cur_block;		/* Current Block */
//14	Bit16u rec_size;		/* Logical record size */
//16	Bit32u filesize;		/* File Size */
//20	Bit16u date;
//22	Bit16u time;
//		/* Reserved Block should be 8 bytes */
//24	Bit8u sft_entries;
//25	Bit8u share_attributes;
//26	Bit8u extra_info;
//27	Bit8u file_handle;
//28	Bit8u reserved[4];
//		/* end */
//32	Bit8u  cur_rec;			/* Current record in current block */
//33	Bit32u rndm;			/* Current relative record number */
//37	}
}
