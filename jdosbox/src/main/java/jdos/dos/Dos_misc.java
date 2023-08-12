package jdos.dos;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.LongRef;

import java.util.Vector;

public class Dos_misc {
    static public Vector Multiplex;

    public static void DOS_AddMultiplexHandler(Dos_system.MultiplexHandler handler) {
        Multiplex.add(handler);
    }

    static public void DOS_DelMultiplexHandler(Dos_system.MultiplexHandler handler) {
        Multiplex.remove(handler);
    }

    static private Callback.Handler INT2F_Handler = new Callback.Handler() {
        public String getName() {
            return "Dos_misc.INT2F_Handler";
        }
        public /*Bitu*/int call() {
            for (int i=0;i<Multiplex.size();i++) {
                Dos_system.MultiplexHandler m = (Dos_system.MultiplexHandler)Multiplex.elementAt(i);
                if (m.call())
                    return Callback.CBRET_NONE;
            }
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC, LogSeverities.LOG_ERROR,"DOS:Multiplex Unhandled call "+Integer.toString(CPU_Regs.reg_eax.word(),16));
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler INT2A_Handler = new Callback.Handler() {
        public String getName() {
            return "Dos_misc.INT2A_Handler";
        }
        public /*Bitu*/int call() {
            return Callback.CBRET_NONE;
        }
    };

    static Dos_system.MultiplexHandler DOS_MultiplexFunctions = new Dos_system.MultiplexHandler() {
        public boolean call() {
            switch (CPU_Regs.reg_eax.word()) {
            case 0x1216:	/* GET ADDRESS OF SYSTEM FILE TABLE ENTRY */
                // reg_bx is a system file table entry, should coincide with
                // the file handle so just use that
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Some BAD filetable call used bx="+Integer.toString(CPU_Regs.reg_ebx.word(),16));
                if(CPU_Regs.reg_ebx.word() <= Dos_files.DOS_FILES) Callback.CALLBACK_SCF(false);
                else Callback.CALLBACK_SCF(true);
                if (CPU_Regs.reg_ebx.word()<16) {
                    /*RealPt*/int sftrealpt= Memory.mem_readd(Memory.Real2Phys(Dos.dos_infoblock.GetPointer()) + 4);
                    /*PhysPt*/int sftptr=Memory.Real2Phys(sftrealpt);
                    /*Bitu*/int sftofs=0x06+CPU_Regs.reg_ebx.word()*0x3b;

                    if (Dos_files.Files[CPU_Regs.reg_ebx.word()]!=null) Memory.mem_writeb(sftptr+sftofs,Dos_files.Files[CPU_Regs.reg_ebx.word()].refCtr);
                    else Memory.mem_writeb(sftptr+sftofs,0);

                    if (Dos_files.Files[CPU_Regs.reg_ebx.word()]==null) return true;

                    /*Bit32u*/long handle=Dos.RealHandle(CPU_Regs.reg_ebx.word());
                    if (handle>=Dos_files.DOS_FILES) {
                        Memory.mem_writew(sftptr+sftofs+0x02,0x02);	// file open mode
                        Memory.mem_writeb(sftptr+sftofs+0x04,0x00);	// file attribute
                        Memory.mem_writew(sftptr+sftofs+0x05,Dos_files.Files[CPU_Regs.reg_ebx.word()].GetInformation());	// device info word
                        Memory.mem_writed(sftptr+sftofs+0x07,0);		// device driver header
                        Memory.mem_writew(sftptr+sftofs+0x0d,0);		// packed time
                        Memory.mem_writew(sftptr+sftofs+0x0f,0);		// packed date
                        Memory.mem_writew(sftptr+sftofs+0x11,0);		// size
                        Memory.mem_writew(sftptr+sftofs+0x15,0);		// current position
                    } else {
                        /*Bit8u*/short drive=Dos_files.Files[CPU_Regs.reg_ebx.word()].GetDrive();

                        Memory.mem_writew(sftptr+sftofs+0x02,(/*Bit16u*/int)(Dos_files.Files[CPU_Regs.reg_ebx.word()].flags&3));	// file open mode
                        Memory.mem_writeb(sftptr+sftofs+0x04,(/*Bit8u*/short)(Dos_files.Files[CPU_Regs.reg_ebx.word()].attr));		// file attribute
                        Memory.mem_writew(sftptr+sftofs+0x05,0x40|drive);							// device info word
                        Memory.mem_writed(sftptr+sftofs+0x07,Memory.RealMake(Dos.dos.tables.dpb,drive));		// dpb of the drive
                        Memory.mem_writew(sftptr+sftofs+0x0d,Dos_files.Files[CPU_Regs.reg_ebx.word()].time);					// packed file time
                        Memory.mem_writew(sftptr+sftofs+0x0f,Dos_files.Files[CPU_Regs.reg_ebx.word()].date);					// packed file date
                        /*Bit32u*/LongRef curpos=new LongRef(0);
                        Dos_files.Files[CPU_Regs.reg_ebx.word()].Seek(curpos,Dos_files.DOS_SEEK_CUR);
                        /*Bit32u*/LongRef endpos=new LongRef(0);
                        Dos_files.Files[CPU_Regs.reg_ebx.word()].Seek(endpos,Dos_files.DOS_SEEK_END);
                        Memory.mem_writed(sftptr+sftofs+0x11,(int)endpos.value);		// size
                        Memory.mem_writed(sftptr+sftofs+0x15,(int)curpos.value);		// current position
                        Dos_files.Files[CPU_Regs.reg_ebx.word()].Seek(curpos,Dos_files.DOS_SEEK_SET);
                    }

                    // fill in filename in fcb style
                    // (space-padded name (8 chars)+space-padded extension (3 chars))
                    String filename=Dos_files.Files[CPU_Regs.reg_ebx.word()].GetName();
                    if (filename.endsWith("\\") || filename.endsWith("/")) return true;
                    if (filename.lastIndexOf('\\')>=0) filename=filename.substring(filename.lastIndexOf('\\')+1);
                    if (filename.lastIndexOf('/')>=0) filename=filename.substring(filename.lastIndexOf('/')+1);
                    int dotpos=filename.lastIndexOf('.');
                    if (dotpos>=0) {
                        dotpos++;
                        int nlen=filename.length();
                        int extlen=filename.length()-dotpos;
                        int nmelen=nlen-extlen;
                        if (nmelen<1) return true;
                        nlen-=(extlen+1);

                        if (nlen>8) nlen=8;
                        int i;

                        for (i=0; i<nlen; i++)
                            Memory.mem_writeb((/*PhysPt*/int)(sftptr+sftofs+0x20+i),filename.charAt(i));
                        for (i=nlen; i<8; i++)
                            Memory.mem_writeb((/*PhysPt*/int)(sftptr+sftofs+0x20+i),' ');

                        if (extlen>3) extlen=3;
                        for (i=0; i<extlen; i++)
                            Memory.mem_writeb((/*PhysPt*/int)(sftptr+sftofs+0x28+i),filename.charAt(dotpos+i));
                        for (i=extlen; i<3; i++)
                            Memory.mem_writeb((/*PhysPt*/int)(sftptr+sftofs+0x28+i),' ');
                    } else {
                        int i;
                        int nlen=filename.length();
                        if (nlen>8) nlen=8;
                        for (i=0; i<nlen; i++)
                            Memory.mem_writeb((/*PhysPt*/int)(sftptr+sftofs+0x20+i),filename.charAt(i));
                        for (i=nlen; i<11; i++)
                            Memory.mem_writeb((/*PhysPt*/int)(sftptr+sftofs+0x20+i),' ');
                    }

                    CPU_Regs.SegSet16ES(Memory.RealSeg(sftrealpt));
                    CPU_Regs.reg_edi.word(Memory.RealOff(sftrealpt+sftofs));
                    CPU_Regs.reg_eax.word(0xc000);
                }
                return true;
            case 0x1607:
                if (CPU_Regs.reg_ebx.word() == 0x15) {
                    switch (CPU_Regs.reg_ecx.word()) {
                        case 0x0000:		// query instance
                            CPU_Regs.reg_ecx.word(0x0001);
                            CPU_Regs.reg_edx.word(0x50);		// dos driver segment
                            CPU_Regs.SegSet16ES(0x50);	// patch table seg
                            CPU_Regs.reg_ebx.word(0x60);		// patch table ofs
                            return true;
                        case 0x0001:		// set patches
                            CPU_Regs.reg_eax.word(0xb97c);
                            CPU_Regs.reg_ebx.word((CPU_Regs.reg_edx.word() & 0x16));
                            CPU_Regs.reg_edx.word(0xa2ab);
                            return true;
                        case 0x0003:		// get size of data struc
                            if (CPU_Regs.reg_edx.word()==0x0001) {
                                // CDS size requested
                                CPU_Regs.reg_eax.word(0xb97c);
                                CPU_Regs.reg_edx.word(0xa2ab);
                                CPU_Regs.reg_ecx.word(0x000e);	// size
                            }
                            return true;
                        case 0x0004:		// instanced data
                            CPU_Regs.reg_edx.word(0);		// none
                            return true;
                        case 0x0005:		// get device driver size
                            CPU_Regs.reg_eax.word(0);
                            CPU_Regs.reg_edx.word(0);
                            return true;
                        default:
                            return false;
                    }
                }
                else if (CPU_Regs.reg_ebx.word() == 0x18) return true;	// idle callout
                else return false;
            case 0x1680:	/*  RELEASE CURRENT VIRTUAL MACHINE TIME-SLICE */
                //TODO Maybe do some idling but could screw up other systems :)
                return true; //So no warning in the debugger anymore
            case 0x1689:	/*  Kernel IDLE CALL */
            case 0x168f:	/*  Close awareness crap */
               /* Removing warning */
                return true;
            case 0x4a01:	/* Query free hma space */
            case 0x4a02:	/* ALLOCATE HMA SPACE */
                Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_WARN,"INT 2f:4a HMA. DOSBox reports none available.");
                CPU_Regs.reg_ebx.word(0);	//number of bytes available in HMA or amount successfully allocated
                //ESDI=ffff:ffff Location of HMA/Allocated memory
                CPU_Regs.SegSet16ES(0xffff);
                CPU_Regs.reg_edi.word(0xffff);
                return true;
            }

            return false;
        }
    };

    static private /*Bitu*/int call_int2f,call_int2a;
    static public void DOS_SetupMisc() {
        Multiplex = new Vector();
        /* Setup the dos multiplex interrupt */
        call_int2f=Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_int2f,INT2F_Handler,Callback.CB_IRET,"DOS Int 2f");
        Memory.RealSetVec(0x2f,Callback.CALLBACK_RealPointer(call_int2f));
        DOS_AddMultiplexHandler(DOS_MultiplexFunctions);
        /* Setup the dos network interrupt */
        call_int2a=Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_int2a,INT2A_Handler,Callback.CB_IRET,"DOS Int 2a");
        Memory.RealSetVec(0x2A,Callback.CALLBACK_RealPointer(call_int2a));
    }

}
