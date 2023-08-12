package jdos.dos;

import jdos.hardware.Memory;

public class Dos_InfoBlock  extends MemStruct {
    public Dos_InfoBlock () {}
    public void SetLocation(/*Bit16u*/int  segment) {
        seg = segment;
        pt=Memory.PhysMake(seg,0);
        /* Clear the initial Block */
        for(/*Bitu*/int i=0;i<sizeofDIB;i++) Memory.mem_writeb(pt+i,0xff);
        for(/*Bitu*/int i=0;i<14;i++) Memory.mem_writeb(pt+i,0);

        SaveIt(2, regCXfrom5e, 0); //sSave(sDIB,regCXfrom5e,(Bit16u)0);
        SaveIt(2, countLRUcache, 0); //sSave(sDIB,countLRUcache,(Bit16u)0);
        SaveIt(2, countLRUopens, 0); //sSave(sDIB,countLRUopens,(Bit16u)0);

        SaveIt(2, protFCBs, 0); //sSave(sDIB,protFCBs,(Bit16u)0);
        SaveIt(2, specialCodeSeg, 0); //sSave(sDIB,specialCodeSeg,(Bit16u)0);
        SaveIt(1, joindedDrives, 0); //sSave(sDIB,joindedDrives,(Bit8u)0);
        SaveIt(1, lastdrive, 0x01); //sSave(sDIB,lastdrive,(Bit8u)0x01);//increase this if you add drives to cds-chain

        SaveIt(4, diskInfoBuffer, (int)Memory.RealMake(segment,diskBufferHeadPt)); //sSave(sDIB,diskInfoBuffer,RealMake(segment,offsetof(sDIB,diskBufferHeadPt)));
        SaveIt(4, setverPtr, 0); //sSave(sDIB,setverPtr,(Bit32u)0);

        SaveIt(2, a20FixOfs, 0); //sSave(sDIB,a20FixOfs,(Bit16u)0);
        SaveIt(2, pspLastIfHMA, 0); //sSave(sDIB,pspLastIfHMA,(Bit16u)0);
        SaveIt(1, blockDevices, 0); //sSave(sDIB,blockDevices,(Bit8u)0);

        SaveIt(1, bootDrive, 0); //sSave(sDIB,bootDrive,(Bit8u)0);
        SaveIt(1, useDwordMov, 1) ; //sSave(sDIB,useDwordMov,(Bit8u)1);
        SaveIt(2, extendedSize, (Memory.MEM_TotalPages()*4-1024)); //sSave(sDIB,extendedSize,(Bit16u)(MEM_TotalPages()*4-1024));
        SaveIt(2, magicWord, 0x0001); //sSave(sDIB,magicWord,(Bit16u)0x0001);		// dos5+

        SaveIt(2, sharingCount, 0); //sSave(sDIB,sharingCount,(Bit16u)0);
        SaveIt(2, sharingDelay, 0); //sSave(sDIB,sharingDelay,(Bit16u)0);
        SaveIt(2, ptrCONinput, 0); //sSave(sDIB,ptrCONinput,(Bit16u)0);			// no unread input available
        SaveIt(2, maxSectorLength, 0x200); //sSave(sDIB,maxSectorLength,(Bit16u)0x200);

        SaveIt(2, dirtyDiskBuffers, 0); //sSave(sDIB,dirtyDiskBuffers,(Bit16u)0);
        SaveIt(4, lookaheadBufPt, 0); //sSave(sDIB,lookaheadBufPt,(Bit32u)0);
        SaveIt(2, lookaheadBufNumber, 0); //sSave(sDIB,lookaheadBufNumber,(Bit16u)0);
        SaveIt(1, bufferLocation, 0); //sSave(sDIB,bufferLocation,(Bit8u)0);		// buffer in base memory, no workspace
        SaveIt(4, workspaceBuffer, 0); //sSave(sDIB,workspaceBuffer,(Bit32u)0);

        SaveIt(2, minMemForExec, 0); //sSave(sDIB,minMemForExec,(Bit16u)0);
        SaveIt(2, memAllocScanStart, 0); //sSave(sDIB,memAllocScanStart,(Bit16u)DOS_MEM_START);
        SaveIt(2, startOfUMBChain, 0xffff); //sSave(sDIB,startOfUMBChain,(Bit16u)0xffff);
        SaveIt(1, chainingUMB, 0); //sSave(sDIB,chainingUMB,(Bit8u)0);

        SaveIt(4, nulNextDriver, 0xFFFFFFFF); //sSave(sDIB,nulNextDriver,(Bit32u)0xffffffff);
        SaveIt(2, nulAttributes, 0x8004); //sSave(sDIB,nulAttributes,(Bit16u)0x8004);
        SaveIt(4, nulStrategy, 0x00000000); //sSave(sDIB,nulStrategy,(Bit32u)0x00000000);
        SaveIt(1, nulString, 0x4e); //sSave(sDIB,nulString[0],(Bit8u)0x4e);
        SaveIt(1, nulString+1, 0x55); //sSave(sDIB,nulString[1],(Bit8u)0x55);
        SaveIt(1, nulString+2, 0x4c); //sSave(sDIB,nulString[2],(Bit8u)0x4c);
        SaveIt(1, nulString+3, 0x20); //sSave(sDIB,nulString[3],(Bit8u)0x20);
        SaveIt(1, nulString+4, 0x20); //sSave(sDIB,nulString[4],(Bit8u)0x20);
        SaveIt(1, nulString+5, 0x20); //sSave(sDIB,nulString[5],(Bit8u)0x20);
        SaveIt(1, nulString+6, 0x20); //sSave(sDIB,nulString[6],(Bit8u)0x20);
        SaveIt(1, nulString+7, 0x20); //sSave(sDIB,nulString[7],(Bit8u)0x20);

        /* Create a fake SFT, so programs think there are 100 file handles */
        /*Bit16u*/short sftOffset=firstFileTable+0xa2;
        SaveIt(4, firstFileTable, (int)Memory.RealMake(segment, sftOffset)); //Save(sDIB,firstFileTable,RealMake(segment,sftOffset));
        Memory.real_writed(segment,sftOffset+0x00,Memory.RealMake(segment+0x26,0));	//Next File Table
        Memory.real_writew(segment,sftOffset+0x04,100);		//File Table supports 100 files
        Memory.real_writed(segment+0x26,0x00,0xffffffff);		//Last File Table
        Memory.real_writew(segment+0x26,0x04,100);				//File Table supports 100 files
    }
    
    public void SetFirstMCB(/*Bit16u*/int firstmcb) {
        SaveIt(2, firstMCB, firstmcb); //(sSave(sDIB,firstMCB,_firstmcb); //c2woody
    }
    
    public void SetBuffers(/*Bit16u*/int x,/*Bit16u*/int y) {
        SaveIt(2, buffers_x, x); //sSave(sDIB,buffers_x,x);
	    SaveIt(2, buffers_y, y); //sSave(sDIB,buffers_y,y);
    }
    
    public void SetCurDirStruct(/*Bit32u*/int _curdirstruct) {
        SaveIt(4, curDirStructure, _curdirstruct); //sSave(sDIB,curDirStructure,_curdirstruct);
    }
    
    public void SetFCBTable(/*Bit32u*/int _fcbtable) {
        SaveIt(4, fcbTable, _fcbtable); //sSave(sDIB,fcbTable,_fcbtable);
    }
    public void SetDeviceChainStart(/*Bit32u*/int _devchain) {
        SaveIt(4, nulNextDriver, _devchain); //sSave(sDIB,nulNextDriver,_devchain);
    }
    public void SetDiskBufferHeadPt(/*Bit32u*/int _dbheadpt) {
        SaveIt(4, diskBufferHeadPt, _dbheadpt); //sSave(sDIB,diskBufferHeadPt,_dbheadpt);
    }
    public void SetStartOfUMBChain(/*Bit16u*/int _umbstartseg) {
        SaveIt(2, startOfUMBChain, _umbstartseg); //sSave(sDIB,startOfUMBChain,_umbstartseg);
    }
    public void SetUMBChainState(/*Bit8u*/short _umbchaining) {
        SaveIt(1, chainingUMB, _umbchaining); //sSave(sDIB,chainingUMB,_umbchaining);
    }
    public /*Bit16u*/int GetStartOfUMBChain() {
        return GetIt(2, startOfUMBChain); //(Bit16u)sGet(sDIB,startOfUMBChain);
    }
    
    public /*Bit8u*/short GetUMBChainState() {
        return (short)GetIt(1, chainingUMB); //(Bit8u)sGet(sDIB,chainingUMB);
    }
    public /*RealPt*/int GetPointer() {
        return Memory.RealMake(seg,firstDPB);
    }
    public /*Bit32u*/int GetDeviceChain() {
        return GetIt(4, nulNextDriver); //(sGet(sDIB,nulNextDriver);
    }


                                                    //    struct sDIB {
                                                    // 0      Bit8u	unknown1[4];
    private static final int magicWord = 4;         // 4      Bit16u	magicWord;			// -0x22 needs to be 1
                                                    // 6      Bit8u	unknown2[8];
    private static final int regCXfrom5e = 14;      // 14     Bit16u	regCXfrom5e;		// -0x18 CX from last int21/ah=5e
    private static final int countLRUcache = 16;    // 16     Bit16u	countLRUcache;		// -0x16 LRU counter for FCB caching
    private static final int countLRUopens = 18;    // 18     Bit16u	countLRUopens;		// -0x14 LRU counter for FCB openings
                                                    // 20     Bit8u	stuff[6];		// -0x12 some stuff, hopefully never used....
    private static final int sharingCount = 26;     // 26     Bit16u	sharingCount;		// -0x0c sharing retry count
    private static final int sharingDelay = 28;     // 28     Bit16u	sharingDelay;		// -0x0a sharing retry delay
                                                    // 30     RealPt	diskBufPtr;		// -0x08 pointer to disk buffer
    private static final int ptrCONinput = 34;      // 34     Bit16u	ptrCONinput;		// -0x04 pointer to con input
    private static final int firstMCB = 36;         // 36     Bit16u	firstMCB;		// -0x02 first memory control block
    private static final int firstDPB = 38;         // 38     RealPt	firstDPB;		//  0x00 first drive parameter block
    private static final int firstFileTable = 42;   // 42     RealPt	firstFileTable;		//  0x04 first system file table
                                                    // 46     RealPt	activeClock;		//  0x08 active clock device header
                                                    // 50     RealPt	activeCon;		//  0x0c active console device header
    private static final int maxSectorLength = 54;  // 54     Bit16u	maxSectorLength;	//  0x10 maximum bytes per sector of any block device;
    private static final int diskInfoBuffer = 56;   // 56     RealPt	diskInfoBuffer;		//  0x12 pointer to disk info buffer
    private static final int curDirStructure = 60;  // 60     RealPt  curDirStructure;	//  0x16 pointer to current array of directory structure
    private static final int fcbTable = 64;         // 64     RealPt	fcbTable;		//  0x1a pointer to system FCB table
    private static final int protFCBs = 68;         // 68     Bit16u	protFCBs;		//  0x1e protected fcbs
    private static final int blockDevices = 70;     // 70     Bit8u	blockDevices;		//  0x20 installed block devices
    private static final int lastdrive = 71;        // 71     Bit8u	lastdrive;		//  0x21 lastdrive
    private static final int nulNextDriver = 72;    // 72     Bit32u	nulNextDriver;	//  0x22 NUL driver next pointer
    private static final int nulAttributes = 76;    // 76     Bit16u	nulAttributes;	//  0x26 NUL driver aattributes
    private static final int nulStrategy = 78;      // 78     Bit32u	nulStrategy;	//  0x28 NUL driver strategy routine
    private static final int nulString = 82;        // 82     Bit8u	nulString[8];	//  0x2c NUL driver name string
    private static final int joindedDrives = 90;    // 90     Bit8u	joindedDrives;		//  0x34 joined drives
    private static final int specialCodeSeg = 91;   // 91     Bit16u	specialCodeSeg;		//  0x35 special code segment
    private static final int setverPtr = 93;        // 93     RealPt  setverPtr;		//  0x37 pointer to setver
    private static final int a20FixOfs = 97;        // 97     Bit16u  a20FixOfs;		//  0x3b a20 fix routine offset
    private static final int pspLastIfHMA = 99;     // 99     Bit16u  pspLastIfHMA;		//  0x3d psp of last program (if dos in hma)
    private static final int buffers_x = 101;       // 101    Bit16u	buffers_x;		//  0x3f x in BUFFERS x,y
    private static final int buffers_y = 103;       // 103    Bit16u	buffers_y;		//  0x41 y in BUFFERS x,y
    private static final int bootDrive = 105;       // 105    Bit8u	bootDrive;		//  0x43 boot drive
    private static final int useDwordMov = 106;     // 106    Bit8u	useDwordMov;		//  0x44 use dword moves
    private static final int extendedSize = 107;    // 107    Bit16u	extendedSize;		//  0x45 size of extended memory
    private static final int diskBufferHeadPt = 109;// 109    Bit32u	diskBufferHeadPt;	//  0x47 pointer to least-recently used buffer header
    private static final int dirtyDiskBuffers = 113;// 113    Bit16u	dirtyDiskBuffers;	//  0x4b number of dirty disk buffers
    private static final int lookaheadBufPt = 115;  // 115    Bit32u	lookaheadBufPt;		//  0x4d pointer to lookahead buffer
    private static final int lookaheadBufNumber=119;// 119    Bit16u	lookaheadBufNumber;		//  0x51 number of lookahead buffers
    private static final int bufferLocation = 121;  // 121    Bit8u	bufferLocation;			//  0x53 workspace buffer location
    private static final int workspaceBuffer = 122; // 122    Bit32u	workspaceBuffer;		//  0x54 pointer to workspace buffer
                                                    // 126    Bit8u	unknown3[11];			//  0x58
    private static final int chainingUMB = 137;     // 137    Bit8u	chainingUMB;			//  0x63 bit0: UMB chain linked to MCB chain
    private static final int minMemForExec = 138;   // 138    Bit16u	minMemForExec;			//  0x64 minimum paragraphs needed for current program
    private static final int startOfUMBChain = 140; // 140    Bit16u	startOfUMBChain;		//  0x66 segment of first UMB-MCB
    private static final int memAllocScanStart =142;// 142    Bit16u	memAllocScanStart;		//  0x68 start paragraph for memory allocation
    private static final int sizeofDIB = 144;       // 144   }
    private /*Bit16u*/int seg;
}
