package jdos.dos;

import jdos.hardware.Memory;
import jdos.util.*;

public class Dos_DTA extends MemStruct {
	public Dos_DTA(/*RealPt*/int addr) { SetRealPt(addr); }

	public void SetupSearch(/*Bit8u*/int _sdrive,/*Bit8u*/int _sattr,String _pattern) {
        SaveIt(1,0, _sdrive);//sSave(sDTA,sdrive,_sdrive);
        SaveIt(1,12,_sattr);//sSave(sDTA,sattr,_sattr);
        /* Fill with spaces */
        /*Bitu*/int i;
        for (i=0;i<11;i++) Memory.mem_writeb(pt+1+i,(short)' ');
        int pos = _pattern.indexOf('.');
        if (pos>=0) {
            Memory.MEM_BlockWrite(pt+1,_pattern.substring(0,pos),8);
            Memory.MEM_BlockWrite(pt+9,_pattern.substring(pos+1),3);
        } else {
            Memory.MEM_BlockWrite(pt+1,_pattern,8);
        }
    }

	public void SetResult(String name,/*Bit32u*/long size,/*Bit16u*/int date,/*Bit16u*/int time,/*Bit8u*/short attr) {
        //MEM_BlockWrite(pt+offsetof(sDTA,name),(void *)_name,DOS_NAMELENGTH_ASCII);
        Memory.MEM_BlockWrite(pt+30, name, 13);
        SaveIt(4,26,(int)size);//sSave(sDTA,size,_size);
        SaveIt(2,24,date);//sSave(sDTA,date,_date);
        SaveIt(2,22,time);//sSave(sDTA,time,_time);
        SaveIt(1,21,attr);//sSave(sDTA,attr,_attr);
    }

	public /*Bit8u*/short GetSearchDrive() {
        return (short)GetIt(1,0);//(Bit8u)sGet(sDTA,sdrive);
    }
    
	public void GetSearchParams(/*Bit8u*/ShortRef attr,StringRef pattern) {
        attr.value=(short)GetIt(1,12);//(Bit8u)sGet(sDTA,sattr);
        byte[] temp = new byte[11];
        Memory.MEM_BlockRead(pt+1, temp, 11);
        pattern.value = StringHelper.toString(temp, 0, 8);
        pattern.value+='.';
        pattern.value+=StringHelper.toString(temp, 8, 3);
    }

	public void GetResult(StringRef _name,/*Bit32u*/LongRef _size,/*Bit16u*/IntRef _date,/*Bit16u*/IntRef _time,/*Bit8u*/ShortRef _attr) {
        byte[] name = new byte[13];
        Memory.MEM_BlockRead(pt+30, name, name.length);
        _name.value = StringHelper.toString(name);
        _size.value=GetIt(4,26);//sGet(sDTA,size);
        _date.value=GetIt(2, 24);//sGet(sDTA,date);
        _time.value=GetIt(2, 22);//sGet(sDTA,time);
        _attr.value=(short)GetIt(1, 21);//sGet(sDTA,attr);
    }

    //#define sSave(s,m,val) SaveIt(sizeof(((s *)&pt)->m),(PhysPt)sOffset(s,m),val)
    //#define sGet(s,m) GetIt(sizeof(((s *)&pt)->m),(PhysPt)sOffset(s,m))
    
	public void	SetDirID(/*Bit16u*/int entry)			{SaveIt(2, 13, entry);}//{ sSave(sDTA,dirID,entry); };
	public void	SetDirIDCluster(/*Bit16u*/int entry)	{SaveIt(2, 15, entry);}//{ sSave(sDTA,dirCluster,entry); };
	public /*Bit16u*/int	GetDirID()				{return GetIt(2, 13);}//{ return (Bit16u)sGet(sDTA,dirID); };
	public /*Bit16u*/int	GetDirIDCluster()		{return GetIt(2,15);}//{ return (Bit16u)sGet(sDTA,dirCluster); };


//	struct sDTA {
//0		Bit8u sdrive;						/* The Drive the search is taking place */
//1		Bit8u sname[8];						/* The Search pattern for the filename */
//9 	Bit8u sext[3];						/* The Search pattern for the extenstion */
//12	Bit8u sattr;						/* The Attributes that need to be found */
//13	Bit16u dirID;						/* custom: dir-search ID for multiple searches at the same time */
//15	Bit16u dirCluster;					/* custom (drive_fat only): cluster number for multiple searches at the same time */
//17	Bit8u fill[4];
//21	Bit8u attr;
//22	Bit16u time;
//24	Bit16u date;
//26	Bit32u size;
//30	char name[DOS_NAMELENGTH_ASCII];
//43
//	} GCC_ATTRIBUTE(packed);
}
