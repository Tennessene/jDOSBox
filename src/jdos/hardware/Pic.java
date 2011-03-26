package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core_normal;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;

public class Pic extends Module_base {
    public interface PIC_EOIHandler {
        public void call();
    }
    public interface PIC_EventHandler {
        public void call(/*Bitu*/int val);
    }
    public static final int PIC_MAXIRQ = 15;
    public static final int PIC_NOIRQ = 0xFF;

    public static float PIC_TickIndex() {
        return (CPU.CPU_CycleMax-CPU.CPU_CycleLeft-CPU.CPU_Cycles)/(float)CPU.CPU_CycleMax;
    }

    public static /*Bits*/int PIC_TickIndexND() {
        return CPU.CPU_CycleMax-CPU.CPU_CycleLeft-CPU.CPU_Cycles;
    }

    public static /*Bits*/int PIC_MakeCycles(double amount) {
        return (/*Bits*/int)(CPU.CPU_CycleMax*amount);
    }

    public static double PIC_FullIndex() {
        return PIC_Ticks+(double)PIC_TickIndex();
    }

    public static void PIC_ActivateIRQ(/*Bitu*/int irq) {
        if( irq < 8 ) {
            irqs[irq].active = true;
            if (!irqs[irq].masked) {
                PIC_IRQCheck|=(1 << irq);
            }
        } else 	if (irq < 16) {
            irqs[irq].active = true;
            PIC_IRQOnSecondPicActive|=(1 << irq);
            if (!irqs[irq].masked && !irqs[2].masked) {
                PIC_IRQCheck|=(1 << irq);
            }
        }
    }
    public static void PIC_DeActivateIRQ(/*Bitu*/int irq) {
        if (irq<16) {
		    irqs[irq].active=false;
            PIC_IRQCheck&=~(1 << irq);
            PIC_IRQOnSecondPicActive&=~(1 << irq);
        }
    }

    public static void PIC_runIRQs() {
        //System.out.println("PIC_runIRQs");
        if (CPU_Regs.GETFLAG(CPU_Regs.IF)==0) return;
        if (PIC_IRQCheck == 0) return;
        if (CPU.cpudecoder == Core_normal.CPU_Core_Normal_Trap_Run) return;

        /*Bitu*/final int[] IRQ_priority_order = { 0,1,2,8,9,10,11,12,13,14,15,3,4,5,6,7 };
        /*Bit16u*/final int[] IRQ_priority_lookup = { 0,1,2,11,12,13,14,15,3,4,5,6,7,8,9,10,16 };
        /*Bit16u*/int activeIRQ = PIC_IRQActive;
        if (activeIRQ == PIC_NOIRQ) activeIRQ = 16;
        /* Get the priority of the active irq */
        /*Bit16u*/int Priority_Active_IRQ = IRQ_priority_lookup[activeIRQ];

        /*Bitu*/int i,j;
        /* j is the priority (walker)
         * i is the irq at the current priority */

        /* If one of the pics is in special mode use a check that cares for that. */
        if(!PIC_Special_Mode) {
            for (j = 0; j < Priority_Active_IRQ; j++) {
                i = IRQ_priority_order[j];
                if (!irqs[i].masked && irqs[i].active) {
                    if(PIC_startIRQ(i)) return;
                }
            }
        } else {	/* Special mode variant */
            for (j = 0; j<= 15; j++) {
                i = IRQ_priority_order[j];
                if ( (j < Priority_Active_IRQ) || (pics[ ((i&8)>>3) ].special) ) {
                    if (!irqs[i].masked && irqs[i].active) {
                        /* the irq line is active. it's not masked and
                         * the irq is allowed priority wise. So let's start it */
                        /* If started successfully return, else go for the next */
                        if(PIC_startIRQ(i)) return;
                    }
                }
            }
        }
    }
    public static boolean PIC_RunQueue() {
        /* Check to see if a new milisecond needs to be started */
        CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
        CPU.CPU_Cycles=0;
        if (CPU.CPU_CycleLeft<=0) {
            return false;
        }
        /* Check the queue for an entry */
        /*Bits*/int index_nd=PIC_TickIndexND();
        InEventService = true;
        while (pic_queue.next_entry!=null && (pic_queue.next_entry.index*CPU.CPU_CycleMax<=index_nd)) {
            PICEntry entry=pic_queue.next_entry;
            pic_queue.next_entry=entry.next;

            srv_lag = entry.index;
            //System.out.println("PIC_RunQueue "+entry.pic_event+" "+String.valueOf(entry.value));
            entry.pic_event.call(entry.value); // call the event handler

            /* Put the entry in the free list */
            entry.next=pic_queue.free_entry;
            pic_queue.free_entry=entry;
        }
        InEventService = false;

        /* Check when to set the new cycle end */
        if (pic_queue.next_entry!=null) {
            /*Bits*/int cycles=(/*Bits*/int)(pic_queue.next_entry.index*CPU.CPU_CycleMax-index_nd);
            if (cycles==0) cycles=1;
            if (cycles<CPU.CPU_CycleLeft) {
                CPU.CPU_Cycles=cycles;
            } else {
                CPU.CPU_Cycles=CPU.CPU_CycleLeft;
            }
        } else CPU.CPU_Cycles=CPU.CPU_CycleLeft;
        CPU.CPU_CycleLeft-=CPU.CPU_Cycles;
        if 	(PIC_IRQCheck!=0)	PIC_runIRQs();
        return true;
    }

    //Delay in milliseconds
    public static void PIC_AddEvent(PIC_EventHandler handler,float delay) {
        PIC_AddEvent(handler, delay, 0);
    }
    public static void PIC_AddEvent(PIC_EventHandler handler,float delay,/*Bitu*/int val/*=0*/) {
        if (pic_queue.free_entry==null) {
		    Log.log(LogTypes.LOG_PIC, LogSeverities.LOG_ERROR,"Event queue full");
		    return;
        }
        PICEntry entry=pic_queue.free_entry;
        if(InEventService) entry.index = delay + srv_lag;
        else entry.index = delay + PIC_TickIndex();

        entry.pic_event=handler;
        entry.value=val;
        pic_queue.free_entry=pic_queue.free_entry.next;
        AddEntry(entry);
    }

    public static void PIC_RemoveEvents(PIC_EventHandler handler) {
        PICEntry entry=pic_queue.next_entry;
	    PICEntry prev_entry = null;

        while (entry!=null) {
            if (entry.pic_event==handler) {
                if (prev_entry != null) {
                    prev_entry.next=entry.next;
                    entry.next=pic_queue.free_entry;
                    pic_queue.free_entry=entry;
                    entry=prev_entry.next;
                    continue;
                } else {
                    pic_queue.next_entry=entry.next;
                    entry.next=pic_queue.free_entry;
                    pic_queue.free_entry=entry;
                    entry=pic_queue.next_entry;
                    continue;
                }
            }
            prev_entry=entry;
            entry=entry.next;
        }
    }

    public static void PIC_RemoveSpecificEvents(PIC_EventHandler handler, /*Bitu*/int val) {
        PICEntry entry=pic_queue.next_entry;
        PICEntry prev_entry = null;

        while (entry != null) {
            if (entry.pic_event == handler && entry.value == val) {
                if (prev_entry!=null) {
                    prev_entry.next=entry.next;
                    entry.next=pic_queue.free_entry;
                    pic_queue.free_entry=entry;
                    entry=prev_entry.next;
                    continue;
                } else {
                    pic_queue.next_entry=entry.next;
                    entry.next=pic_queue.free_entry;
                    pic_queue.free_entry=entry;
                    entry=pic_queue.next_entry;
                    continue;
                }
            }
            prev_entry=entry;
            entry=entry.next;
        }
    }

    public static void PIC_SetIRQMask(/*Bitu*/int irq, boolean masked) {
        if(irqs[irq].masked == masked) return;	/* Do nothing if mask doesn't change */
        boolean old_irq2_mask = irqs[2].masked;
        irqs[irq].masked=masked;
        if(irq < 8) {
            if (irqs[irq].active && !irqs[irq].masked) {
                PIC_IRQCheck|=(1 << (irq));
            } else {
                PIC_IRQCheck&=~(1 << (irq));
            }
        } else {
            if (irqs[irq].active && !irqs[irq].masked && !irqs[2].masked) {
                PIC_IRQCheck|=(1 << (irq));
            } else {
                PIC_IRQCheck&=~(1 << (irq));
            }
        }
        if(irqs[2].masked != old_irq2_mask) {
        /* Irq 2 mask has changed recheck second pic */
            for(/*Bitu*/int i=8;i<=15;i++) {
                if (irqs[i].active && !irqs[i].masked && !irqs[2].masked) PIC_IRQCheck|=(1 << (i));
                else PIC_IRQCheck&=~(1 << (i));
            }
        }
        if (PIC_IRQCheck!=0) {
            CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
            CPU.CPU_Cycles=0;
        }
    }

    static private int PIC_QUEUESIZE = 512;
    
    static private class IRQ_Block {
        boolean masked;
        boolean active;
        boolean inservice;
        /*Bitu*/int vector;
    }
    static private class PIC_Controller {
        /*Bitu*/int icw_words;
        /*Bitu*/int icw_index;
        /*Bitu*/int masked;

        boolean special;
        boolean auto_eoi;
        boolean rotate_on_auto_eoi;
        boolean single;
        boolean request_issr;
        /*Bit8u*/short vector_base;
    }

    static public /*Bitu*/int PIC_Ticks;
    static public /*Bitu*/int PIC_IRQCheck;
    static public /*Bitu*/int PIC_IRQOnSecondPicActive;
    static public /*Bitu*/int PIC_IRQActive;

    static private IRQ_Block[] irqs = new IRQ_Block[16];
    static private PIC_Controller[] pics = new PIC_Controller[2];
    static private boolean PIC_Special_Mode = false; //Saves one compare in the pic_run_irqloop

    static public class PICEntry {
        double index;
        /*Bitu*/int value;
        PIC_EventHandler pic_event;
        PICEntry next;
    }
    static public class Pic_queue {
        public Pic_queue() {
            for (int i=0;i<entries.length;i++)
                entries[i] = new PICEntry();
        }
        PICEntry[] entries = new PICEntry[PIC_QUEUESIZE];
        PICEntry free_entry;
        PICEntry next_entry;
    }
    
    static public Pic_queue pic_queue;

    static private IoHandler.IO_WriteHandler write_command = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            PIC_Controller pic=pics[port==0x20 ? 0 : 1];
            /*Bitu*/int irq_base=port==0x20 ? 0 : 8;
            /*Bitu*/int i;
            final /*Bit16u*/int[] IRQ_priority_table = { 0,1,2,8,9,10,11,12,13,14,15,3,4,5,6,7 };
            if ((val&0x10)!=0) {		// ICW1 issued
                if ((val&0x04)!=0) Log.exit("PIC: 4 byte interval not handled");
                if ((val&0x08)!=0) Log.exit("PIC: level triggered mode not handled");
                if ((val&0xe0)!=0) Log.exit("PIC: 8080/8085 mode not handled");
                pic.single=(val&0x02)==0x02;
                pic.icw_index=1;			// next is ICW2
                pic.icw_words=2 + (val&0x01);	// =3 if ICW4 needed
            } else if ((val&0x08)!=0) {	// OCW3 issued
                if ((val&0x04)!=0) Log.exit("PIC: poll command not handled");
                if ((val&0x02)!=0) {		// function select
                    if ((val&0x01)!=0) pic.request_issr=true;	/* select read interrupt in-service register */
                    else pic.request_issr=false;			/* select read interrupt request register */
                }
                if ((val&0x40)!=0) {		// special mask select
                    if ((val&0x20)!=0) pic.special=true;
                    else pic.special=false;
                    if(pics[0].special || pics[1].special) // :TODO: what was the original intention of if(pic[0].special || pics[1].special) :update Qbix from dosbox forum says that it should have and s
                        PIC_Special_Mode = true; else
                        PIC_Special_Mode = false;
                    if (PIC_IRQCheck!=0) { //Recheck irqs
                        CPU.CPU_CycleLeft += CPU.CPU_Cycles;
                        CPU.CPU_Cycles = 0;
                    }
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,"port "+Integer.toString(port, 16)+" : special mask "+((pic.special)?"ON":"OFF"));
                }
            } else {	// OCW2 issued
                if ((val&0x20)!=0) {		// EOI commands
                    if ((val&0x80)!=0) Log.exit("rotate mode not supported");
                    if ((val&0x40)!=0) {		// specific EOI
                        if (PIC_IRQActive==(irq_base+val-0x60)) {
                            irqs[PIC_IRQActive].inservice=false;
                            PIC_IRQActive=PIC_NOIRQ;
                            for (i=0; i<=15; i++) {
                                if (irqs[IRQ_priority_table[i]].inservice) {
                                    PIC_IRQActive=IRQ_priority_table[i];
                                    break;
                                }
                            }
                        }
        //				if (val&0x80);	// perform rotation
                    } else {		// nonspecific EOI
                        if (PIC_IRQActive<(irq_base+8)) {
                            irqs[PIC_IRQActive].inservice=false;
                            PIC_IRQActive=PIC_NOIRQ;
                            for (i=0; i<=15; i++){
                                if(irqs[IRQ_priority_table[i]].inservice) {
                                    PIC_IRQActive=IRQ_priority_table[i];
                                    break;
                                }
                            }
                        }
        //				if (val&0x80);	// perform rotation
                    }
                } else {
                    if ((val&0x40)==0) {		// rotate in auto EOI mode
                        if ((val&0x80)!=0) pic.rotate_on_auto_eoi=true;
                        else pic.rotate_on_auto_eoi=false;
                    } else if ((val&0x80)!=0) {
                        Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,"set priority command not handled");
                    }	// else NOP command
                }
            }	// end OCW2
        }
    };

    static private IoHandler.IO_WriteHandler write_data = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            PIC_Controller pic=pics[port==0x21 ? 0 : 1];
            /*Bitu*/int irq_base=(port==0x21) ? 0 : 8;
            /*Bitu*/int i;
            boolean old_irq2_mask = irqs[2].masked;
            switch(pic.icw_index) {
            case 0:                        /* mask register */
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,(port==0x21 ? 0 : 1)+" mask "+Integer.toString(val,16));
                for (i=0;i<=7;i++) {
                    irqs[i+irq_base].masked=(val&(1<<i))>0;
                    if(port==0x21) {
                        if (irqs[i+irq_base].active && !irqs[i+irq_base].masked) PIC_IRQCheck|=(1 << (i+irq_base));
                        else PIC_IRQCheck&=~(1 << (i+irq_base));
                    } else {
                        if (irqs[i+irq_base].active && !irqs[i+irq_base].masked && !irqs[2].masked) PIC_IRQCheck|=(1 << (i+irq_base));
                        else PIC_IRQCheck&=~(1 << (i+irq_base));
                    }
                }
                if (Dosbox.machine== MachineType.MCH_PCJR) {
                    /* irq6 cannot be disabled as it serves as pseudo-NMI */
                    irqs[6].masked=false;
                }
                if(irqs[2].masked != old_irq2_mask) {
                /* Irq 2 mask has changed recheck second pic */
                    for(i=8;i<=15;i++) {
                        if (irqs[i].active && !irqs[i].masked && !irqs[2].masked) PIC_IRQCheck|=(1 << (i));
                        else PIC_IRQCheck&=~(1 << (i));
                    }
                }
                if (PIC_IRQCheck!=0) {
                    CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
                    CPU.CPU_Cycles=0;
                }
                break;
            case 1:                        /* icw2          */
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,(port==0x21 ? 0 : 1)+":Base vector "+Integer.toString(val,16));
                for (i=0;i<=7;i++) {
                    irqs[i+irq_base].vector=(val&0xf8)+i;
                };
                if(pic.icw_index++ >= pic.icw_words) pic.icw_index=0;
                else if(pic.single) pic.icw_index=3;		/* skip ICW3 in single mode */
                break;
            case 2:							/* icw 3 */
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,(port==0x21 ? 0 : 1)+":ICW 3 "+Integer.toString(val,16));
                if(pic.icw_index++ >= pic.icw_words) pic.icw_index=0;
                break;
            case 3:							/* icw 4 */
                /*
                    0	    1 8086/8080  0 mcs-8085 mode
                    1	    1 Auto EOI   0 Normal EOI
                    2-3	   0x Non buffer Mode
                           10 Buffer Mode Slave
                           11 Buffer mode Master
                    4		Special/Not Special nested mode
                */
                pic.auto_eoi=(val & 0x2)>0;

                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,(port==0x21 ? 0 : 1)+":ICW 4 "+Integer.toString(val,16));

                if ((val&0x01)==0) Log.exit("PIC:ICW4: "+Integer.toString(val, 16)+", 8085 mode not handled");
                if ((val&0x10)!=0) Log.log_msg("PIC:ICW4: "+Integer.toString(val, 16)+", special fully-nested mode not handled");

                if(pic.icw_index++ >= pic.icw_words) pic.icw_index=0;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,"ICW HUH? "+Integer.toString(val,16));
                break;
            }
        }
    };

    static private IoHandler.IO_ReadHandler read_command = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            PIC_Controller pic=pics[port==0x20 ? 0 : 1];
            /*Bitu*/int irq_base=(port==0x20) ? 0 : 8;
            /*Bitu*/int i;/*Bit8u*/int ret=0;/*Bit8u*/int b=1;
            if (pic.request_issr) {
                for (i=irq_base;i<irq_base+8;i++) {
                    if (irqs[i].inservice) ret|=b;
                    b <<= 1;
                }
            } else {
                for (i=irq_base;i<irq_base+8;i++) {
                    if (irqs[i].active)	ret|=b;
                    b <<= 1;
                }
                if (irq_base==0 && (PIC_IRQCheck&0xff00)!=0) ret |=4;
            }
            return ret;
        }
    };

    static private IoHandler.IO_ReadHandler read_data = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bitu*/int irq_base=(port==0x21) ? 0 : 8;
            /*Bitu*/int i;/*Bit8u*/int ret=0;/*Bit8u*/int b=1;
            for (i=irq_base;i<=irq_base+7;i++) {
                if (irqs[i].masked)	ret|=b;
                b <<= 1;
            }
            return ret;
        }
    };

    static private boolean PIC_startIRQ(/*Bitu*/int i) {
        //System.out.println("PIC_startIRQ "+i);
        /* irqs on second pic only if irq 2 isn't masked */
        if( i > 7 && irqs[2].masked) return false;
        irqs[i].active = false;
        PIC_IRQCheck&= ~(1 << i);
        PIC_IRQOnSecondPicActive&= ~(1 << i);
        CPU.CPU_HW_Interrupt(irqs[i].vector);
        /*Bitu*/int pic=(i&8)>>3;
        if (!pics[pic].auto_eoi) { //irq 0-7 => pic 0 else pic 1
            PIC_IRQActive = i;
            irqs[i].inservice = true;
        } else if (pics[pic].rotate_on_auto_eoi) {
            Log.exit("rotate on auto EOI not handled");
        }
        return true;
    }

    static private void AddEntry(PICEntry entry) {
        PICEntry find_entry=pic_queue.next_entry;
        if (find_entry == null) {
            entry.next=null;
            pic_queue.next_entry=entry;
        } else if (find_entry.index>entry.index) {
            pic_queue.next_entry=entry;
            entry.next=find_entry;
        } else {
            while (find_entry!=null) {
                if (find_entry.next!=null) {
                    /* See if the next index comes later than this one */
                    if (find_entry.next.index > entry.index) {
                        entry.next=find_entry.next;
                        find_entry.next=entry;
                        break;
                    } else {
                        find_entry=find_entry.next;
                    }
                } else {
                    entry.next=find_entry.next;
                    find_entry.next=entry;
                    break;
                }
            }
        }
        /*Bits*/int cycles=PIC_MakeCycles(pic_queue.next_entry.index-PIC_TickIndex());
        if (cycles<CPU.CPU_Cycles) {
            CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
            CPU.CPU_Cycles=0;
        }
    }

    static boolean InEventService = false;
    static double srv_lag = 0;

    IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[4];
	IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[4];

    public Pic(Section configuration) {
        super(configuration);
        PIC_IRQCheck=0;
		PIC_IRQActive=PIC_NOIRQ;
		PIC_Ticks=0;
		/*Bitu*/int i;
        for (i=0;i<ReadHandler.length;i++)
            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        for (i=0;i<WriteHandler.length;i++)
            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
		for (i=0;i<2;i++) {
			pics[i].masked=0xff;
			pics[i].auto_eoi=false;
			pics[i].rotate_on_auto_eoi=false;
			pics[i].request_issr=false;
			pics[i].special=false;
			pics[i].single=false;
			pics[i].icw_index=0;
			pics[i].icw_words=0;
		}
		for (i=0;i<=7;i++) {
			irqs[i].active=false;
			irqs[i].masked=true;
			irqs[i].inservice=false;
			irqs[i+8].active=false;
			irqs[i+8].masked=true;
			irqs[i+8].inservice=false;
			irqs[i].vector=0x8+i;
			irqs[i+8].vector=0x70+i;
		}
		irqs[0].masked=false;					/* Enable system timer */
		irqs[1].masked=false;					/* Enable Keyboard IRQ */
		irqs[2].masked=false;					/* Enable second pic */
		irqs[8].masked=false;					/* Enable RTC IRQ */
		if (Dosbox.machine==MachineType.MCH_PCJR) {
			/* Enable IRQ6 (replacement for the NMI for PCJr) */
			irqs[6].masked=false;
		}
		ReadHandler[0].Install(0x20,read_command,IoHandler.IO_MB);
		ReadHandler[1].Install(0x21,read_data,IoHandler.IO_MB);
		WriteHandler[0].Install(0x20,write_command,IoHandler.IO_MB);
		WriteHandler[1].Install(0x21,write_data,IoHandler.IO_MB);
		ReadHandler[2].Install(0xa0,read_command,IoHandler.IO_MB);
		ReadHandler[3].Install(0xa1,read_data,IoHandler.IO_MB);
		WriteHandler[2].Install(0xa0,write_command,IoHandler.IO_MB);
		WriteHandler[3].Install(0xa1,write_data,IoHandler.IO_MB);
		/* Initialize the pic queue */
		for (i=0;i<PIC_QUEUESIZE-1;i++) {
			pic_queue.entries[i].next=pic_queue.entries[i+1];
		}
		pic_queue.entries[PIC_QUEUESIZE-1].next=null;
		pic_queue.free_entry=pic_queue.entries[0];
		pic_queue.next_entry=null;
    }

    static Pic test;

    public static Section.SectionFunction PIC_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
            for (int i=0;i<irqs.length;i++)
                irqs[i] = null;
            for (int i=0;i<pics.length;i++)
                pics[i] = null;
            pic_queue = null;
        }
    };
    
    public static Section.SectionFunction PIC_Init = new Section.SectionFunction() {
        public void call(Section section) {
            pic_queue = new Pic_queue();
            for (int i=0;i<irqs.length;i++)
                irqs[i] = new IRQ_Block();
            for (int i=0;i<pics.length;i++)
                pics[i] = new PIC_Controller();
            test = new Pic(section);
            section.AddDestroyFunction(PIC_Destroy);
        }
    };
}
