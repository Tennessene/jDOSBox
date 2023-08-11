package jdos.dos;

import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.util.Ptr;

public class Dos_tables {
    static private final int DOS_PRIVATE_SEGMENT = 0xc800;
    static private final int DOS_PRIVATE_SEGMENT_END = 0xd000;

    private static class DOS_TableCase {
        public /*Bit16u*/short size;
        public byte[] chars = new byte[256];
    }

    /*RealPt*/int DOS_TableUpCase;
    /*RealPt*/int DOS_TableLowCase;

    static /*Bitu*/int call_casemap;

    static private int dos_memseg=DOS_PRIVATE_SEGMENT;

    static public /*Bit16u*/int DOS_GetMemory(/*Bit16u*/int pages) {
        if (pages+dos_memseg>=DOS_PRIVATE_SEGMENT_END) {
            Log.exit("DOS:Not enough memory for internal tables");
        }
        /*Bit16u*/int page=dos_memseg;
        dos_memseg+=pages;
        return page;
    }

    static private Callback.Handler DOS_CaseMapFunc = new Callback.Handler() {
        public String getName() {
            return "Dos_tables.DOS_CaseMapFunc";
        }
        public /*Bitu*/int call() {
            //LOG(LOG_DOSMISC,LOG_ERROR)("Case map routine called : %c",reg_al);
            return Callback.CBRET_NONE;
        }
    };

    static private byte[] country_info = {
    /* Date format      */  0x00, 0x00,
    /* Currencystring   */  0x24, 0x00, 0x00, 0x00, 0x00,
    /* Thousands sep    */  0x2c, 0x00,
    /* Decimal sep      */  0x2e, 0x00,
    /* Date sep         */  0x2d, 0x00,
    /* time sep         */  0x3a, 0x00,
    /* currency form    */  0x00,
    /* digits after dec */  0x02,
    /* Time format      */  0x00,
    /* Casemap          */  0x00, 0x00, 0x00, 0x00,
    /* Data sep         */  0x2c, 0x00,
    /* Reservered 5     */  0x00, 0x00, 0x00, 0x00, 0x00,
    /* Reservered 5     */  0x00, 0x00, 0x00, 0x00, 0x00
    };

    static public void DOS_SetupTables() {
        /*Bit16u*/int seg;/*Bitu*/int i;
        Dos.dos.tables.mediaid= Memory.RealMake(DOS_GetMemory(4),0);
        Dos.dos.tables.tempdta=Memory.RealMake(DOS_GetMemory(4),0);
        Dos.dos.tables.tempdta_fcbdelete=Memory.RealMake(DOS_GetMemory(4),0);
        for (i=0;i<Dos_files.DOS_DRIVES;i++) Memory.mem_writew(Memory.Real2Phys(Dos.dos.tables.mediaid)+i*2,0);
        /* Create the DOS Info Block */
        Dos.dos_infoblock.SetLocation(Dos.DOS_INFOBLOCK_SEG); //c2woody

        /* create SDA */
        new Dos_SDA(Dos.DOS_SDA_SEG,0).Init();

        /* Some weird files >20 detection routine */
        /* Possibly obselete when SFT is properly handled */
        Memory.real_writed(Dos.DOS_CONSTRING_SEG,0x0a,0x204e4f43);
        Memory.real_writed(Dos.DOS_CONSTRING_SEG,0x1a,0x204e4f43);
        Memory.real_writed(Dos.DOS_CONSTRING_SEG,0x2a,0x204e4f43);

        /* create a CON device driver */
        seg=Dos.DOS_CONDRV_SEG;
        Memory.real_writed(seg,0x00,0xffffffff);	// next ptr
        Memory.real_writew(seg,0x04,0x8013);		// attributes
        Memory.real_writed(seg,0x06,0xffffffff);	// strategy routine
        Memory.real_writed(seg,0x0a,0x204e4f43);	// driver name
        Memory.real_writed(seg,0x0e,0x20202020);	// driver name
        Dos.dos_infoblock.SetDeviceChainStart((int)Memory.RealMake(seg,0));

        /* Create a fake Current Directory Structure */
        seg=Dos.DOS_CDS_SEG;
        Memory.real_writed(seg,0x00,0x005c3a43);
        Dos.dos_infoblock.SetCurDirStruct((int)Memory.RealMake(seg,0));



        /* Allocate DCBS DOUBLE BYTE CHARACTER SET LEAD-BYTE TABLE */
        Dos.dos.tables.dbcs=Memory.RealMake(DOS_GetMemory(12),0);
        Memory.mem_writed(Memory.Real2Phys(Dos.dos.tables.dbcs),0); //empty table
        /* FILENAME CHARACTER TABLE */
        Dos.dos.tables.filenamechar=Memory.RealMake(DOS_GetMemory(2),0);
        Memory.mem_writew(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x00,0x16);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x02,0x01);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x03,0x00);	// allowed chars from
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x04,0xff);	// ...to
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x05,0x00);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x06,0x00);	// excluded chars from
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x07,0x20);	// ...to
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x08,0x02);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x09,0x0e);	// number of illegal separators
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x0a,0x2e);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x0b,0x22);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x0c,0x2f);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x0d,0x5c);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x0e,0x5b);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x0f,0x5d);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x10,0x3a);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x11,0x7c);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x12,0x3c);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x13,0x3e);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x14,0x2b);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x15,0x3d);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x16,0x3b);
        Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.filenamechar)+0x17,0x2c);
        /* COLLATING SEQUENCE TABLE + UPCASE TABLE*/
        // 256 bytes for col table, 128 for upcase, 4 for number of entries
        Dos.dos.tables.collatingseq=Memory.RealMake(DOS_GetMemory(25),0);
        Memory.mem_writew(Memory.Real2Phys(Dos.dos.tables.collatingseq),0x100);
        for (i=0; i<256; i++) Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.collatingseq)+i+2,i);
        Dos.dos.tables.upcase=Dos.dos.tables.collatingseq+258;
        Memory.mem_writew(Memory.Real2Phys(Dos.dos.tables.upcase),0x80);
        for (i=0; i<128; i++) Memory.mem_writeb(Memory.Real2Phys(Dos.dos.tables.upcase)+i+2,0x80+i);


        /* Create a fake FCB SFT */
        seg=DOS_GetMemory(4);
        Memory.real_writed(seg,0,0xffffffff);		//Last File Table
        Memory.real_writew(seg,4,100);				//File Table supports 100 files
        Dos.dos_infoblock.SetFCBTable((int)Memory.RealMake(seg,0));

        /* Create a fake DPB */
        Dos.dos.tables.dpb=DOS_GetMemory(2);
        for(/*Bitu*/int d=0;d<26;d++) Memory.real_writeb(Dos.dos.tables.dpb,d,d);

        /* Create a fake disk buffer head */
        seg=DOS_GetMemory(6);
        for (/*Bitu*/int ct=0; ct<0x20; ct++) Memory.real_writeb(seg,ct,0);
        Memory.real_writew(seg,0x00,0xffff);		// forward ptr
        Memory.real_writew(seg,0x02,0xffff);		// backward ptr
        Memory.real_writeb(seg,0x04,0xff);			// not in use
        Memory.real_writeb(seg,0x0a,0x01);			// number of FATs
        Memory.real_writed(seg,0x0d,0xffffffff);	// pointer to DPB
        Dos.dos_infoblock.SetDiskBufferHeadPt((int)Memory.RealMake(seg,0));

        /* Set buffers to a nice value */
        Dos.dos_infoblock.SetBuffers(50,50);

        /* case map routine INT 0x21 0x38 */
        call_casemap = Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_casemap,DOS_CaseMapFunc,Callback.CB_RETF,"DOS CaseMap");
        /* Add it to country structure */
        new Ptr(country_info, 0).writed(0x12, Callback.CALLBACK_RealPointer(call_casemap));
        Dos.dos.tables.country=country_info;
    }
}
