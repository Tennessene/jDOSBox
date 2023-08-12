package jdos.dos;

import jdos.hardware.Memory;

public class Dos_SDA extends MemStruct {
	public Dos_SDA(/*Bit16u*/int _seg,/*Bit16u*/int _offs) { SetPt(_seg,_offs); }
	public void Init() {
        /* Clear */
        for(/*Bitu*/int i=0;i<26;i++) Memory.mem_writeb(pt+i,0x00);
	    SaveIt(1,2, 0xFF); // sSave(sSDA,drive_crit_error,0xff);   
    }
	public void SetDrive(/*Bit8u*/short _drive) { SaveIt(1, 22, _drive);} // sSave(sSDA,current_drive, _drive); }
	public void SetDTA(/*Bit32u*/int _dta) { SaveIt(4, 12, _dta);} //sSave(sSDA,current_dta, _dta); }
	public void SetPSP(/*Bit16u*/int _psp) { SaveIt(2, 16, _psp);} //sSave(sSDA,current_psp, _psp); }
	public /*Bit8u*/short GetDrive() { return (short)GetIt(1, 22);} //(Bit8u)sGet(sSDA,current_drive); }
	public /*Bit16u*/int GetPSP() { return GetIt(2, 16); } // (Bit16u)sGet(sSDA,current_psp); }
	public /*Bit32u*/int GetDTA() { return GetIt(4, 12); } //(Bit32u)sGet(sSDA,current_dta); }

//	private static class sSDA {
//0		Bit8u crit_error_flag;		/* 0x00 Critical Error Flag */
//1		Bit8u inDOS_flag;		/* 0x01 InDOS flag (count of active INT 21 calls) */
//2		Bit8u drive_crit_error;		/* 0x02 Drive on which current critical error occurred or FFh */
//3		Bit8u locus_of_last_error;	/* 0x03 locus of last error */
//4		Bit16u extended_error_code;	/* 0x04 extended error code of last error */
//6		Bit8u suggested_action;		/* 0x06 suggested action for last error */
//7		Bit8u error_class;		/* 0x07 class of last error*/
//8		Bit32u last_error_pointer; 	/* 0x08 ES:DI pointer for last error */
//12	Bit32u current_dta;		/* 0x0C current DTA (Disk Transfer Address) */
//16	Bit16u current_psp; 		/* 0x10 current PSP */
//18	Bit16u sp_int_23;		/* 0x12 stores SP across an INT 23 */
//20	Bit16u return_code;		/* 0x14 return code from last process termination (zerod after reading with AH=4Dh) */
//22	Bit8u current_drive;		/* 0x16 current drive */
//23	Bit8u extended_break_flag; 	/* 0x17 extended break flag */
//24	Bit8u fill[2];			/* 0x18 flag: code page switching || flag: copy of previous byte in case of INT 24 Abort*/
//26}
}
