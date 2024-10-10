package jdos.dos;

import jdos.hardware.Memory;

public class Dos_PSP extends MemStruct {
	public Dos_PSP(/*Bit16u*/int segment) {
        SetPt(segment);seg=segment;
    }

	public void MakeNew(/*Bit16u*/int mem_size) {
        /* get previous */
    //	DOS_PSP prevpsp(dos.psp());
        /* Clear it first */
        /*Bitu*/int i;
        for (i=0;i<256;i++) Memory.mem_writeb(pt+i,0);
        // Set size
        SaveIt(2,2,seg+mem_size); //sSave(sPSP,next_seg,seg+mem_size);

        /* far call opcode */
        SaveIt(1,5,0xea); //sSave(sPSP,far_call,0xea);
        // far call to interrupt 0x21 - faked for bill & ted
        // lets hope nobody really uses this address
        SaveIt(4,6,(int)Memory.RealMake(0xDEAD,0xFFFF)); //sSave(sPSP,cpm_entry,RealMake(0xDEAD,0xFFFF));
        /* Standard blocks,int 20  and int21 retf */
        SaveIt(1, 0, 0xcd); //sSave(sPSP,exit[0],0xcd);
        SaveIt(1, 1, 0x20); //sSave(sPSP,exit[1],0x20);
        SaveIt(1, 80, 0xcd); //sSave(sPSP,service[0],0xcd);
        SaveIt(1, 81, 0x21); //sSave(sPSP,service[1],0x21);
        SaveIt(1, 82, 0xcb); //sSave(sPSP,service[2],0xcb);
        /* psp and psp-parent */
        SaveIt(2, 22, Dos.dos.psp()); //sSave(sPSP,psp_parent,dos.psp());
        SaveIt(4, 56, 0xffffffff); //sSave(sPSP,prev_psp,0xffffffff);
        SaveIt(2, 64, 0x0005); //sSave(sPSP,dos_version,0x0005);
        /* terminate 22,break 23,crititcal error 24 address stored */
        SaveVectors();

        /* FCBs are filled with 0 */
        // ....
        /* Init file pointer and max_files */
        SaveIt(4, 52, (int)Memory.RealMake(seg,24)); //sSave(sPSP,file_table,RealMake(seg,offsetof(sPSP,files)));
        SaveIt(2, 50, 20); //sSave(sPSP,max_files,20);
        for (/*Bit16u*/int ct=0;ct<20;ct++) SetFileHandle(ct,0xff);

        /* User Stack pointer */
    //	if (prevpsp.GetSegment()!=0) sSave(sPSP,stack,prevpsp.GetStack());

        if (rootpsp==0) rootpsp = seg;
    }

	public void CopyFileTable(Dos_PSP srcpsp,boolean createchildpsp) {
        /* Copy file table from calling process */
        for (/*Bit16u*/int i=0;i<20;i++) {
            /*Bit8u*/int handle = srcpsp.GetFileHandle(i);
            if(createchildpsp)
            {	//copy obeying not inherit flag.(but dont duplicate them)
                boolean allowCopy = true;//(handle==0) || ((handle>0) && (FindEntryByHandle(handle)==0xff));
                if((handle<Dos_files.DOS_FILES) && Dos_files.Files[handle]!=null && (Dos_files.Files[handle].flags & Dos_files.DOS_NOT_INHERIT)==0 && allowCopy)
                {
                    Dos_files.Files[handle].AddRef();
                    SetFileHandle(i,handle);
                }
                else
                {
                    SetFileHandle(i,0xff);
                }
            }
            else
            {	//normal copy so don't mind the inheritance
                SetFileHandle(i,handle);
            }
        }
    }

	public /*Bit16u*/int FindFreeFileEntry() {
        /*PhysPt*/int files=Memory.Real2Phys(GetIt(4,52) /*sGet(sPSP,file_table)*/);
        for (/*Bit16u*/int i=0;i<GetIt(2, 50) /*sGet(sPSP,max_files)*/;i++) {
            if (Memory.mem_readb(files+i)==0xff) return i;
        }	
        return 0xff;
    }

	public void	CloseFiles() {
        for (/*Bit16u*/int i=0;i<GetIt(2, 50) /*sGet(sPSP,max_files)*/;i++) {
            Dos_files.DOS_CloseFile(i);
        }
    }

	public void	SaveVectors () {
        /* Save interrupt 22,23,24 */
        SaveIt(4, 10, (int)Memory.RealGetVec(0x22));//sSave(sPSP,int_22,RealGetVec(0x22));
        SaveIt(4, 14, (int)Memory.RealGetVec(0x23));//sSave(sPSP,int_23,RealGetVec(0x23));
        SaveIt(4, 18, (int)Memory.RealGetVec(0x24));//sSave(sPSP,int_24,RealGetVec(0x24));
    }

	public void	RestoreVectors() {
        /* Restore interrupt 22,23,24 */
        Memory.RealSetVec(0x22,GetIt(4,10)/*sGet(sPSP,int_22)*/);
        Memory.RealSetVec(0x23,GetIt(4,14)/*sGet(sPSP,int_23)*/);
        Memory.RealSetVec(0x24,GetIt(4,18)/*sGet(sPSP,int_24)*/);
    }

	public void	SetSize(/*Bit16u*/int size) {
        SaveIt(2,2,size);//sSave(sPSP,next_seg,size);
    }

	public /*Bit16u*/int GetSize() {
        return GetIt(2,2);//sGet(sPSP,next_seg);
    }

	public void	SetEnvironment(/*Bit16u*/int envseg) {
        SaveIt(2,44, envseg); //sSave(sPSP,environment,envseg);
    }

	public /*Bit16u*/int GetEnvironment() {
        return GetIt(2,44); //sGet(sPSP,environment);
    }

	public /*Bit16u*/int GetSegment() {
        return seg;
    }

	public void	SetFileHandle(/*Bit16u*/int index, /*Bit8u*/int handle) {
        if (index<GetIt(2, 50)/*sGet(sPSP,max_files)*/) {
            /*PhysPt*/int files=Memory.Real2Phys(GetIt(4,52)/*sGet(sPSP,file_table)*/);
            Memory.mem_writeb(files+index,handle);
        }
    }

	public /*Bit8u*/int GetFileHandle (/*Bit16u*/int index) {
        if (index>=GetIt(2, 50)/*sGet(sPSP,max_files)*/) return 0xff;
        /*PhysPt*/int files=Memory.Real2Phys(GetIt(4,52)/*sGet(sPSP,file_table)*/);
        return Memory.mem_readb(files+index);
    }

	public void	SetParent(/*Bit16u*/int parent) {
        SaveIt(2,22,parent);//sSave(sPSP,psp_parent,parent);
    }

	public /*Bit16u*/int GetParent() {
        return (/*Bit16u*/int)GetIt(2,22);//sGet(sPSP,psp_parent);
    }

	public void	SetStack(/*RealPt*/int stackpt) {
        SaveIt(4,46,(int)stackpt); //sSave(sPSP,stack,stackpt);
    }

	public /*RealPt*/int GetStack() {
        return GetIt(4,46);//sGet(sPSP,stack);
    }

	public void	SetInt22(/*RealPt*/int int22pt) {
        SaveIt(4,10,(int)int22pt);//sSave(sPSP,int_22,int22pt);
    }

	public /*RealPt*/int GetInt22() {
        return GetIt(4,10);//sGet(sPSP,int_22);
    }

	public void	SetFCB1(/*RealPt*/int src) {
        if (src!=0) Memory.MEM_BlockCopy(Memory.PhysMake(seg,92/*offsetof(sPSP,fcb1)*/),Memory.Real2Phys(src),16);
    }

	public void	SetFCB2(/*RealPt*/int src) {
        if (src!=0) Memory.MEM_BlockCopy(Memory.PhysMake(seg,108/*offsetof(sPSP,fcb2)*/),Memory.Real2Phys(src),16);
    }

	public void	SetCommandTail(/*RealPt*/int src) {
        if (src!=0) {	// valid source
            Memory.MEM_BlockCopy(pt+128/*offsetof(sPSP,cmdtail)*/,Memory.Real2Phys(src),128);
        } else {	// empty
            SaveIt(1,128,0); //sSave(sPSP,cmdtail.count,0x00);
            Memory.mem_writeb(pt+129/*offsetof(sPSP,cmdtail.buffer)*/,0x0d);
        }
    }

	public boolean	SetNumFiles(/*Bit16u*/int fileNum) {
        //20 minimum. clipper program.
	    if (fileNum < 20) fileNum = 20;

        if (fileNum>20) {
            // Allocate needed paragraphs
            fileNum+=2;	// Add a few more files for safety
            /*Bit16u*/int para = (fileNum/16)+((fileNum%16)>0?1:0);
            /*RealPt*/int data	= Memory.RealMake(Dos_tables.DOS_GetMemory(para),0);
            SaveIt(4, 52, (int)data); //sSave(sPSP,file_table,data);
            SaveIt(2, 50, fileNum); //sSave(sPSP,max_files,fileNum);
            /*Bit16u*/int i;
            for (i=0; i<20; i++)		SetFileHandle(i,GetIt(1,24+i)/*(Bit8u)sGet(sPSP,files[i])*/);
            for (i=20; i<fileNum; i++)	SetFileHandle(i,0xFF);
        } else {
            SaveIt(2, 50, fileNum);//sSave(sPSP,max_files,fileNum);
        }
        return true;
    }

	public /*Bit16u*/int FindEntryByHandle(/*Bit8u*/short handle) {
        /*PhysPt*/int files=Memory.Real2Phys(GetIt(4,50)/*sGet(sPSP,file_table)*/);
        for (/*Bit16u*/int i=0;i<GetIt(2,50)/*sGet(sPSP,max_files)*/;i++) {
            if (Memory.mem_readb(files+i)==handle) return i;
        }
        return 0xFF;
    }

//    struct CommandTail{
//      Bit8u count;				/* number of bytes returned */
//      char buffer[127];			 /* the buffer itself */
//    }

//	struct sPSP {
//0		Bit8u	exit[2];			/* CP/M-like exit poimt */
//2		Bit16u	next_seg;			/* Segment of first byte beyond memory allocated or program */
//4		Bit8u	fill_1;				/* single char fill */
//5		Bit8u	far_call;			/* far call opcode */
//6		RealPt	cpm_entry;			/* CPM Service Request address*/
//10	RealPt	int_22;				/* Terminate Address */
//14	RealPt	int_23;				/* Break Address */
//18	RealPt	int_24;				/* Critical Error Address */
//22	Bit16u	psp_parent;			/* Parent PSP Segment */
//24	Bit8u	files[20];			/* File Table - 0xff is unused */
//44	Bit16u	environment;		/* Segment of evironment table */
//46	RealPt	stack;				/* SS:SP Save point for int 0x21 calls */
//50	Bit16u	max_files;			/* Maximum open files */
//52	RealPt	file_table;			/* Pointer to File Table PSP:0x18 */
//56	RealPt	prev_psp;			/* Pointer to previous PSP */
//60	Bit8u interim_flag;
//61	Bit8u truename_flag;
//62	Bit16u nn_flags;
//64	Bit16u dos_version;
//66	Bit8u	fill_2[14];			/* Lot's of unused stuff i can't care aboue */
//80	Bit8u	service[3];			/* INT 0x21 Service call int 0x21;retf; */
//83	Bit8u	fill_3[9];			/* This has some blocks with FCB info */
//92	Bit8u	fcb1[16];			/* first FCB */
//108	Bit8u	fcb2[16];			/* second FCB */
//124	Bit8u	fill_4[4];			/* unused */
//128	CommandTail cmdtail;
//256	} GCC_ATTRIBUTE(packed);
	private /*Bit16u*/int seg;
    public static /*Bit16u*/int rootpsp;
}
