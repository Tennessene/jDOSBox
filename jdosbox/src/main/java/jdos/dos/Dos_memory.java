package jdos.dos;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;
import jdos.util.StringRef;

public class Dos_memory {
    static public final int MCB_FREE=0x0000;
    static public final int MCB_DOS=0x0008;

    static private final int UMB_START_SEG = 0x9fff;

    static private /*Bit16u*/int memAllocStrategy = 0x00;

    static private void DOS_CompressMemory() {
        /*Bit16u*/int mcb_segment=Dos.dos.firstMCB;
        Dos_MCB mcb = new Dos_MCB(mcb_segment);
        Dos_MCB mcb_next = new Dos_MCB(0);

        while (mcb.GetType()!=0x5a) {
            mcb_next.SetPt((/*Bit16u*/int)(mcb_segment+mcb.GetSize()+1));
            if ((mcb.GetPSPSeg()==0) && (mcb_next.GetPSPSeg()==0)) {
                mcb.SetSize(mcb.GetSize()+mcb_next.GetSize()+1);
                mcb.SetType(mcb_next.GetType());
            } else {
                mcb_segment+=mcb.GetSize()+1;
                mcb.SetPt(mcb_segment);
            }
        }
    }

    static public void DOS_FreeProcessMemory(/*Bit16u*/int pspseg) {
        /*Bit16u*/int mcb_segment=Dos.dos.firstMCB;
        Dos_MCB mcb=new Dos_MCB(mcb_segment);
        for (;;) {
            if (mcb.GetPSPSeg()==pspseg) {
                mcb.SetPSPSeg(MCB_FREE);
            }
            if (mcb.GetType()==0x5a) {
                /* check if currently last block reaches up to the PCJr graphics memory */
                if ((Dosbox.machine== MachineType.MCH_PCJR) && (mcb_segment+mcb.GetSize()==0x17fe) &&
                   (Memory.real_readb(0x17ff,0)==0x4d) && (Memory.real_readw(0x17ff,1)==8)) {
                    /* re-enable the memory past segment 0x2000 */
                    mcb.SetType((short)0x4d);
                } else break;
            }
            mcb_segment+=mcb.GetSize()+1;
            mcb.SetPt(mcb_segment);
        }

        /*Bit16u*/int umb_start=Dos.dos_infoblock.GetStartOfUMBChain();
        if (umb_start==UMB_START_SEG) {
            Dos_MCB umb_mcb=new Dos_MCB(umb_start);
            for (;;) {
                if (umb_mcb.GetPSPSeg()==pspseg) {
                    umb_mcb.SetPSPSeg(MCB_FREE);
                }
                if (umb_mcb.GetType()!=0x4d) break;
                umb_start+=umb_mcb.GetSize()+1;
                umb_mcb.SetPt(umb_start);
            }
        } else if (umb_start!=0xffff) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC, LogSeverities.LOG_ERROR,"Corrupt UMB chain: "+Integer.toString(umb_start,16));

        DOS_CompressMemory();
    }

    public static /*Bit16u*/int DOS_GetMemAllocStrategy() {
        return memAllocStrategy;
    }

    public static boolean DOS_SetMemAllocStrategy(/*Bit16u*/int strat) {
        if ((strat&0x3f)<3) {
            memAllocStrategy = strat;
            return true;
        }
        /* otherwise an invalid allocation strategy was specified */
        return false;
    }

    public static boolean DOS_AllocateMemory(/*Bit16u*/IntRef segment,/*Bit16u*/IntRef blocks) {
        DOS_CompressMemory();
        /*Bit16u*/int bigsize=0;
        /*Bit16u*/int mem_strat=memAllocStrategy;
        /*Bit16u*/int mcb_segment=Dos.dos.firstMCB;

        /*Bit16u*/int umb_start=Dos.dos_infoblock.GetStartOfUMBChain();
        if (umb_start==UMB_START_SEG) {
            /* start with UMBs if requested (bits 7 or 6 set) */
            if ((mem_strat&0xc0)!=0) mcb_segment=umb_start;
        } else if (umb_start!=0xffff) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Corrupt UMB chain: "+Integer.toString(umb_start,16));

        Dos_MCB mcb=new Dos_MCB(0);
        Dos_MCB mcb_next=new Dos_MCB(0);
        Dos_MCB psp_mcb=new Dos_MCB(Dos.dos.psp()-1);
        StringRef psp_name = new StringRef();
        psp_mcb.GetFileName(psp_name);
        /*Bit16u*/int found_seg=0,found_seg_size=0;
        for (;;) {
            mcb.SetPt(mcb_segment);
            if (mcb.GetPSPSeg()==0) {
                /* Check for enough free memory in current block */
                /*Bit16u*/int block_size=mcb.GetSize();
                if (block_size<blocks.value) {
                    if (bigsize<block_size) {
                        /* current block is largest block that was found,
                           but still not as big as requested */
                        bigsize=block_size;
                    }
                } else if ((block_size==blocks.value) && ((mem_strat & 0x3f)<2)) {
                    /* MCB fits precisely, use it if search strategy is firstfit or bestfit */
                    mcb.SetPSPSeg(Dos.dos.psp());
                    segment.value=mcb_segment+1;
                    return true;
                } else {
                    switch (mem_strat & 0x3f) {
                        case 0: /* firstfit */
                            mcb_next.SetPt((/*Bit16u*/int)(mcb_segment+blocks.value+1));
                            mcb_next.SetPSPSeg(MCB_FREE);
                            mcb_next.SetType(mcb.GetType());
                            mcb_next.SetSize(block_size-blocks.value-1);
                            mcb.SetSize(blocks.value);
                            mcb.SetType((short)0x4d);
                            mcb.SetPSPSeg(Dos.dos.psp());
                            mcb.SetFileName(psp_name.value);
                            //TODO Filename
                            segment.value=mcb_segment+1;
                            return true;
                        case 1: /* bestfit */
                            if ((found_seg_size==0) || (block_size<found_seg_size)) {
                                /* first fitting MCB, or smaller than the last that was found */
                                found_seg=mcb_segment;
                                found_seg_size=block_size;
                            }
                            break;
                        default: /* everything else is handled as lastfit by dos */
                            /* MCB is large enough, note it down */
                            found_seg=mcb_segment;
                            found_seg_size=block_size;
                            break;
                    }
                }
            }
            /* Onward to the next MCB if there is one */
            if (mcb.GetType()==0x5a) {
                if ((mem_strat&0x80)!=0 && (umb_start==UMB_START_SEG)) {
                    /* bit 7 set: try high memory first, then low */
                    mcb_segment=Dos.dos.firstMCB;
                    mem_strat&=(~0xc0);
                } else {
                    /* finished searching all requested MCB chains */
                    if (found_seg!=0) {
                        /* a matching MCB was found (cannot occur for firstfit) */
                        if ((mem_strat & 0x3f)==0x01) {
                            /* bestfit, allocate block at the beginning of the MCB */
                            mcb.SetPt(found_seg);

                            mcb_next.SetPt((/*Bit16u*/int)(found_seg+blocks.value+1));
                            mcb_next.SetPSPSeg(MCB_FREE);
                            mcb_next.SetType(mcb.GetType());
                            mcb_next.SetSize(found_seg_size-blocks.value-1);

                            mcb.SetSize(blocks.value);
                            mcb.SetType((short)0x4d);
                            mcb.SetPSPSeg(Dos.dos.psp());
                            mcb.SetFileName(psp_name.value);
                            //TODO Filename
                            segment.value=found_seg+1;
                        } else {
                            /* lastfit, allocate block at the end of the MCB */
                            mcb.SetPt(found_seg);
                            if (found_seg_size==blocks.value) {
                                /* use the whole block */
                                mcb.SetPSPSeg(Dos.dos.psp());
                                //Not consistent with line 124. But how many application will use this information ?
                                mcb.SetFileName(psp_name.value);
                                segment.value = found_seg+1;
                                return true;
                            }
                            segment.value = found_seg+1+found_seg_size - blocks.value;
                            mcb_next.SetPt((/*Bit16u*/int)(segment.value-1));
                            mcb_next.SetSize(blocks.value);
                            mcb_next.SetType(mcb.GetType());
                            mcb_next.SetPSPSeg(Dos.dos.psp());
                            mcb_next.SetFileName(psp_name.value);
                            // Old Block
                            mcb.SetSize(found_seg_size-blocks.value-1);
                            mcb.SetPSPSeg(MCB_FREE);
                            mcb.SetType((short)0x4D);
                        }
                        return true;
                    }
                    /* no fitting MCB found, return size of largest block */
                    blocks.value=bigsize;
                    Dos.DOS_SetError(Dos.DOSERR_INSUFFICIENT_MEMORY);
                    return false;
                }
            } else mcb_segment+=mcb.GetSize()+1;
        }
    }


    static public boolean DOS_ResizeMemory(/*Bit16u*/int segment,/*Bit16u*/IntRef blocks) {
        if (segment < Dos.DOS_MEM_START+1) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Program resizes "+Integer.toString(segment, 16)+", take care");
        }

        Dos_MCB mcb=new Dos_MCB(segment-1);
        if ((mcb.GetType()!=0x4d) && (mcb.GetType()!=0x5a)) {
            Dos.DOS_SetError(Dos.DOSERR_MCB_DESTROYED);
            return false;
        }

        DOS_CompressMemory();
        /*Bit16u*/int total=mcb.GetSize();
        Dos_MCB	mcb_next=new Dos_MCB(segment+total);
        if (blocks.value<=total) {
            if (blocks.value==total) {
                /* Nothing to do */
                return true;
            }
            /* Shrinking MCB */
            Dos_MCB	mcb_new_next=new Dos_MCB(segment+(blocks.value));
            mcb.SetSize(blocks.value);
            mcb_new_next.SetType(mcb.GetType());
            if (mcb.GetType()==0x5a) {
                /* Further blocks follow */
                mcb.SetType((short)0x4d);
            }

            mcb_new_next.SetSize(total-blocks.value-1);
            mcb_new_next.SetPSPSeg(MCB_FREE);
            mcb.SetPSPSeg(Dos.dos.psp());
            return true;
        }
        /* MCB will grow, try to join with following MCB */
        if (mcb.GetType()!=0x5a) {
            if (mcb_next.GetPSPSeg()==MCB_FREE) {
                total+=mcb_next.GetSize()+1;
            }
        }
        if (blocks.value<total) {
            if (mcb.GetType()!=0x5a) {
                /* save type of following MCB */
                mcb.SetType(mcb_next.GetType());
            }
            mcb.SetSize(blocks.value);
            mcb_next.SetPt((/*Bit16u*/int)(segment+blocks.value));
            mcb_next.SetSize(total-blocks.value-1);
            mcb_next.SetType(mcb.GetType());
            mcb_next.SetPSPSeg(MCB_FREE);
            mcb.SetType((short)0x4d);
            mcb.SetPSPSeg(Dos.dos.psp());
            return true;
        }

        /* at this point: *blocks==total (fits) or *blocks>total,
           in the second case resize block to maximum */

        if ((mcb_next.GetPSPSeg()==MCB_FREE) && (mcb.GetType()!=0x5a)) {
            /* adjust type of joined MCB */
            mcb.SetType(mcb_next.GetType());
        }
        mcb.SetSize(total);
        mcb.SetPSPSeg(Dos.dos.psp());
        if (blocks.value==total) return true;	/* block fit exactly */

        blocks.value=total;	/* return maximum */
        Dos.DOS_SetError(Dos.DOSERR_INSUFFICIENT_MEMORY);
        return false;
    }


    static public boolean DOS_FreeMemory(/*Bit16u*/int segment) {
    //TODO Check if allowed to free this segment
        if (segment < Dos.DOS_MEM_START+1) {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Program tried to free "+Integer.toString(segment, 16)+" ---ERROR");
            Dos.DOS_SetError(Dos.DOSERR_MB_ADDRESS_INVALID);
            return false;
        }

        Dos_MCB mcb=new Dos_MCB(segment-1);
        if ((mcb.GetType()!=0x4d) && (mcb.GetType()!=0x5a)) {
            Dos.DOS_SetError(Dos.DOSERR_MB_ADDRESS_INVALID);
            return false;
        }
        mcb.SetPSPSeg(MCB_FREE);
    //	DOS_CompressMemory();
        return true;
    }


    static public void DOS_BuildUMBChain(boolean umb_active,boolean ems_active) {
        if (umb_active  && (Dosbox.machine!=MachineType.MCH_TANDY)) {
            /*Bit16u*/int first_umb_seg = 0xd000;
            /*Bit16u*/int first_umb_size = 0x2000;
            if(ems_active || (Dosbox.machine == MachineType.MCH_PCJR)) first_umb_size = 0x1000;

            Dos.dos_infoblock.SetStartOfUMBChain(UMB_START_SEG);
            Dos.dos_infoblock.SetUMBChainState((short)0);		// UMBs not linked yet

            Dos_MCB umb_mcb=new Dos_MCB(first_umb_seg);
            umb_mcb.SetPSPSeg(0);		// currently free
            umb_mcb.SetSize(first_umb_size-1);
            umb_mcb.SetType((short)0x5a);

            /* Scan MCB-chain for last block */
            /*Bit16u*/int mcb_segment=Dos.dos.firstMCB;
            Dos_MCB mcb=new Dos_MCB(mcb_segment);
            while (mcb.GetType()!=0x5a) {
                mcb_segment+=mcb.GetSize()+1;
                mcb.SetPt(mcb_segment);
            }

            /* A system MCB has to cover the space between the
               regular MCB-chain and the UMBs */
            /*Bit16u*/int cover_mcb=(/*Bit16u*/int)(mcb_segment+mcb.GetSize()+1);
            mcb.SetPt(cover_mcb);
            mcb.SetType((short)0x4d);
            mcb.SetPSPSeg(0x0008);
            mcb.SetSize(first_umb_seg-cover_mcb-1);
            mcb.SetFileName("SC      ");

        } else {
            Dos.dos_infoblock.SetStartOfUMBChain(0xffff);
            Dos.dos_infoblock.SetUMBChainState((short)0);
        }
    }

    static public boolean DOS_LinkUMBsToMemChain(/*Bit16u*/int linkstate) {
        /* Get start of UMB-chain */
        /*Bit16u*/int umb_start=Dos.dos_infoblock.GetStartOfUMBChain();
        if (umb_start!=UMB_START_SEG) {
            if (umb_start!=0xffff) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"Corrupt UMB chain: "+Integer.toString(umb_start,16));
            return false;
        }

        if ((linkstate&1)==(Dos.dos_infoblock.GetUMBChainState()&1)) return true;

        /* Scan MCB-chain for last block before UMB-chain */
        /*Bit16u*/int mcb_segment=Dos.dos.firstMCB;
        /*Bit16u*/int prev_mcb_segment=Dos.dos.firstMCB;
        Dos_MCB mcb=new Dos_MCB(mcb_segment);
        while ((mcb_segment!=umb_start) && (mcb.GetType()!=0x5a)) {
            prev_mcb_segment=mcb_segment;
            mcb_segment+=mcb.GetSize()+1;
            mcb.SetPt(mcb_segment);
        }
        Dos_MCB prev_mcb=new Dos_MCB(prev_mcb_segment);

        switch (linkstate) {
            case 0x0000:	// unlink
                if ((prev_mcb.GetType()==0x4d) && (mcb_segment==umb_start)) {
                    prev_mcb.SetType((short)0x5a);
                }
                Dos.dos_infoblock.SetUMBChainState((short)0);
                break;
            case 0x0001:	// link
                if (mcb.GetType()==0x5a) {
                    mcb.SetType((short)0x4d);
                    Dos.dos_infoblock.SetUMBChainState((short)1);
                }
                break;
            default:
                Log.log_msg("Invalid link state "+Integer.toString(linkstate, 16)+" when reconfiguring MCB chain");
                return false;
        }

        return true;
    }

    static private Callback.Handler DOS_default_handler = new Callback.Handler() {
        public String getName() {
            return "Dos_memory.DOS_default_handler";
        }
        public /*Bitu*/int call() {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"DOS rerouted Interrupt Called "+Integer.toString(CPU.lastint,16));
            return Callback.CBRET_NONE;
        }
    };

    static private Callback callbackhandler;
    static public void DOS_SetupMemory() {
        callbackhandler = new Callback();
        /* Let dos claim a few bios interrupts. Makes DOSBox more compatible with
         * buggy games, which compare against the interrupt table. (probably a
         * broken linked list implementation) */
        callbackhandler.Allocate(DOS_default_handler,"DOS default int");
        /*Bit16u*/int ihseg = 0x70;
        /*Bit16u*/int ihofs = 0x08;
        Memory.real_writeb(ihseg,ihofs+0x00,0xFE);	//GRP 4
        Memory.real_writeb(ihseg,ihofs+0x01,0x38);	//Extra Callback instruction
        Memory.real_writew(ihseg,ihofs+0x02,callbackhandler.Get_callback());  //The immediate word
        Memory.real_writeb(ihseg,ihofs+0x04,0xCF);	//An IRET Instruction
        Memory.RealSetVec(0x01,Memory.RealMake(ihseg,ihofs));		//BioMenace (offset!=4)
        Memory.RealSetVec(0x02,Memory.RealMake(ihseg,ihofs));		//BioMenace (segment<0x8000)
        Memory.RealSetVec(0x03,Memory.RealMake(ihseg,ihofs));		//Alien Incident (offset!=0)
        Memory.RealSetVec(0x04,Memory.RealMake(ihseg,ihofs));		//Shadow President (lower byte of segment!=0)
    //	RealSetVec(0x0f,RealMake(ihseg,ihofs));		//Always a tricky one (soundblaster irq)

        // Create a dummy device MCB with PSPSeg=0x0008
        Dos_MCB mcb_devicedummy=new Dos_MCB((/*Bit16u*/int)Dos.DOS_MEM_START);
        mcb_devicedummy.SetPSPSeg(MCB_DOS);	// Devices
        mcb_devicedummy.SetSize(1);
        mcb_devicedummy.SetType((short)0x4d);		// More blocks will follow
    //	mcb_devicedummy.SetFileName("SD      ");

        /*Bit16u*/int mcb_sizes=2;
        // Create a small empty MCB (result from a growing environment block)
        Dos_MCB tempmcb=new Dos_MCB((/*Bit16u*/int)Dos.DOS_MEM_START+mcb_sizes);
        tempmcb.SetPSPSeg(MCB_FREE);
        tempmcb.SetSize(4);
        mcb_sizes+=5;
        tempmcb.SetType((short)0x4d);

        // Lock the previous empty MCB
        Dos_MCB tempmcb2=new Dos_MCB((/*Bit16u*/int)Dos.DOS_MEM_START+mcb_sizes);
        tempmcb2.SetPSPSeg(0x40);	// can be removed by loadfix
        tempmcb2.SetSize(16);
        mcb_sizes+=17;
        tempmcb2.SetType((short)0x4d);

        Dos_MCB mcb=new Dos_MCB((/*Bit16u*/int)Dos.DOS_MEM_START+mcb_sizes);
        mcb.SetPSPSeg(MCB_FREE);						//Free
        mcb.SetType((short)0x5a);								//Last Block
        if (Dosbox.machine==MachineType.MCH_TANDY) {
            /* memory up to 608k available, the rest (to 640k) is used by
                the tandy graphics system's variable mapping of 0xb800 */
            mcb.SetSize(0x9BFF - Dos.DOS_MEM_START - mcb_sizes);
        } else if (Dosbox.machine==MachineType.MCH_PCJR) {
            /* memory from 128k to 640k is available */
            mcb_devicedummy.SetPt((/*Bit16u*/int)0x2000);
            mcb_devicedummy.SetPSPSeg(MCB_FREE);
            mcb_devicedummy.SetSize(0x9FFF - 0x2000);
            mcb_devicedummy.SetType((short)0x5a);

            /* exclude PCJr graphics region */
            mcb_devicedummy.SetPt((/*Bit16u*/int)0x17ff);
            mcb_devicedummy.SetPSPSeg(MCB_DOS);
            mcb_devicedummy.SetSize(0x800);
            mcb_devicedummy.SetType((short)0x4d);

            /* memory below 96k */
            mcb.SetSize(0x1800 - Dos.DOS_MEM_START - (2+mcb_sizes));
            mcb.SetType((short)0x4d);
        } else {
            /* complete memory up to 640k available */
            /* last paragraph used to add UMB chain to low-memory MCB chain */
            mcb.SetSize(0x9FFE - Dos.DOS_MEM_START - mcb_sizes);
        }

        Dos.dos.firstMCB=Dos.DOS_MEM_START;
        Dos.dos_infoblock.SetFirstMCB(Dos.DOS_MEM_START);
    }

}
