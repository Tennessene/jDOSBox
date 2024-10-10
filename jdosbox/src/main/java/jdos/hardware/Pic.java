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
    public interface PIC_EventHandler {
        public void call(/*Bitu*/int val);
    }

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
        /*Bitu*/int t = irq>7 ? (irq - 8): irq;
        PIC_Controller pic = pics[irq>7 ? 1 : 0];

        /*Bit32s*/int OldCycles = CPU.CPU_Cycles;
        pic.raise_irq(t); //Will set the CPU_Cycles to zero if this IRQ will be handled directly

        if (OldCycles != CPU.CPU_Cycles) {
            // if CPU_Cycles have changed, this means that the interrupt was triggered by an I/O
            // register write rather than an event.
            // Real hardware executes 0 to ~13 NOPs or comparable instructions
            // before the processor picks up the interrupt. Let's try with 2
            // cycles here.
            // Required by Panic demo (irq0), It came from the desert (MPU401)
            // Does it matter if CPU_CycleLeft becomes negative?

            // It might be an idea to do this always in order to simulate this
            // So on write mask and EOI as well. (so inside the activate function)
    //		CPU_CycleLeft += (CPU_Cycles-2);
            CPU.CPU_CycleLeft -= 2;
            CPU.CPU_Cycles = 2;
        }
    }

    public static void PIC_DeActivateIRQ(/*Bitu*/int irq) {
        /*Bitu*/int t = irq>7 ? (irq - 8): irq;
        PIC_Controller pic = pics[irq>7 ? 1 : 0];
        pic.lower_irq(t);
    }

    static void slave_startIRQ(){
        /*Bit8u*/int pic1_irq = 8;
        final int p = (slave.irr & slave.imrr)&slave.isrr;
        final int max = slave.special?8:slave.active_irq;
        for(/*Bit8u*/int i = 0,s = 1;i < max;i++, s<<=1){
            if ((p & s)!=0){
                pic1_irq = i;
                break;
            }
        }
        // Maybe change the E_Exit to a return
        if (pic1_irq == 8) Log.exit("irq 2 is active, but no irq active on the slave PIC.");

        slave.start_irq(pic1_irq);
        master.start_irq(2);
        CPU.CPU_HW_Interrupt(slave.vector_base + pic1_irq);
    }

    static void master_startIRQ(/*Bitu*/int i){
        master.start_irq(i);
        CPU.CPU_HW_Interrupt(master.vector_base + i);
    }

    public static void PIC_runIRQs() {
        if (CPU_Regs.GETFLAG(CPU_Regs.IF)==0) return;
        if (PIC_IRQCheck==0) return;
        if (CPU.cpudecoder==Core_normal.CPU_Core_Normal_Trap_Run) return;

        final int p = (master.irr & master.imrr)&master.isrr;
        final int max = master.special?8:master.active_irq;
        for(/*Bit8u*/int i = 0,s = 1;i < max;i++, s<<=1){
            if ((p & s)!=0) {
                if (i==2) { //second pic
                    slave_startIRQ();
                } else {
                    master_startIRQ(i);
                }
                break;
            }
        }
        //Disable check variable.
        PIC_IRQCheck = 0;
    }

    public static boolean PIC_RunQueue() {
        /* Check to see if a new millisecond needs to be started */
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
        /*Bitu*/int t = irq>7 ? (irq - 8): irq;
        PIC_Controller pic=pics[irq>7 ? 1 : 0];
        //clear bit
        /*Bit8u*/int bit = 1 <<(t);
        /*Bit8u*/int newmask = pic.imr;
        newmask &= ~bit;
        if (masked) newmask |= bit;
        pic.set_imr(newmask);
    }

    static private int PIC_QUEUESIZE = 512;
    
    static private class PIC_Controller {
        /*Bitu*/int icw_words;
        /*Bitu*/int icw_index;

        boolean special;
        boolean auto_eoi;
        boolean rotate_on_auto_eoi;
        boolean single;
        boolean request_issr;
        /*Bit8u*/int vector_base;

        /*Bit8u*/int irr;        // request register
	    /*Bit8u*/int imr;        // mask register
	    /*Bit8u*/int imrr;       // mask register reversed (makes bit tests simpler)
	    /*Bit8u*/int isr;        // in service register
	    /*Bit8u*/int isrr;       // in service register reversed (makes bit tests simpler)
	    /*Bit8u*/int active_irq; //currently active irq

        void set_imr(/*Bit8u*/int val) {
            if (Dosbox.machine==MachineType.MCH_PCJR) {
                //irq 6 is a NMI on the PCJR
                if (this == master) val &= ~(1 <<(6));
            }
            /*Bit8u*/int change = (imr) ^ (val); //Bits that have changed become 1.
            imr  =  val;
            imrr = (~val) & 0xFF;
            //Test if changed bits are set in irr and are not being served at the moment
            //Those bits have impact on whether the cpu emulation should be paused or not.
            if (((irr & change) & isrr)!=0) check_for_irq();
        }

        void check_after_EOI(){
            //Update the active_irq as an EOI is likely to change that.
            update_active_irq();
            if (((irr & imrr) & isrr)!=0) check_for_irq();
        }

        void update_active_irq() {
            if(isr == 0) {active_irq = 8; return;}
            for(int i = 0, s = 1; i < 8;i++, s<<=1){
                if ((isr & s)!=0) {
                    active_irq = i;
                    return;
                }
            }
        }

        void check_for_irq(){
            final /*Bit8u*/int possible_irq = (irr&imrr)&isrr;
            if (possible_irq!=0) {
                final /*Bit8u*/int a_irq = special?8:active_irq;
                for(int i = 0, s = 1; i < a_irq;i++, s<<=1){
                    if ((possible_irq & s)!=0) {
                        //There is an irq ready to be served => signal master and/or cpu
                        activate();
                        return;
                    }
                }
            }
            deactivate(); //No irq, remove signal to master and/or cpu
        }

        //Signals master/cpu that there is an irq ready.
        void activate() {
            //Stops CPU if master, signals master if slave
            if(this == master) {
                PIC_IRQCheck = 1;
                //cycles 0, take care of the port IO stuff added in raise_irq base caller.
                CPU.CPU_CycleLeft += CPU.CPU_Cycles;
                CPU.CPU_Cycles = 0;
                //maybe when coming from a EOI, give a tiny delay. (for the cpu to pick it up) (see PIC_Activate_IRQ)
            } else {
                master.raise_irq(2);
            }
        }

        //Removes signal to master/cpu that there is an irq ready.
        void deactivate() {
            //removes irq check value  if master, signals master if slave
            if(this == master) {
                PIC_IRQCheck = 0;
            } else {
                master.lower_irq(2);
            }
        }

        void raise_irq(/*Bit8u*/int val){
            /*Bit8u*/int bit = 1 << (val);
            if((irr & bit)==0) { //value changed (as it is currently not active)
                irr|=bit;
                if (((bit & imrr) & isrr)!=0) { //not masked and not in service
                    if(special || val < active_irq) activate();
                }
            }
        }

        void lower_irq(/*Bit8u*/int val){
            /*Bit8u*/int bit = 1 << ( val);
            if ((irr & bit)!=0) { //value will change (as it is currently active)
                irr&=~bit;
                if (((bit & imrr) & isrr)!=0) { //not masked and not in service
                    //This irq might have toggled PIC_IRQCheck/caused irq 2 on master, when it was raised.
                    //If it is active, then recheck it, we can't just deactivate as there might be more IRQS raised.
                    if(special || val < active_irq) check_for_irq();
                }
            }
        }

        //handles all bits and logic related to starting this IRQ, it does NOT start the interrupt on the CPU.
        void start_irq(/*Bit8u*/int val) {
            irr&=~(1<<(val));
            if (!auto_eoi) {
                active_irq = val;
                isr |= 1<<(val);
                isrr = ~isr;
            } else if (rotate_on_auto_eoi) {
                Log.exit("rotate on auto EOI not handled");
            }
        }
    }

    static private final PIC_Controller[] pics = new PIC_Controller[2];
    static private PIC_Controller master = null;
    static private PIC_Controller slave  = null;
    static public /*Bitu*/int PIC_Ticks = 0;
    static public /*Bitu*/int PIC_IRQCheck = 0; //Maybe make it a bool and/or ensure 32bit size (x86 dynamic core seems to assume 32 bit variable size)

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
                    //Check if there are irqs ready to run, as the priority system has possibly been changed.
			        pic.check_for_irq();
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,"port "+Integer.toString(port, 16)+" : special mask "+((pic.special)?"ON":"OFF"));
                }
            } else {	// OCW2 issued
                if ((val&0x20)!=0) {		// EOI commands
                    if ((val&0x80)!=0) Log.exit("rotate mode not supported");
                    if ((val&0x40)!=0) {		// specific EOI
                        pic.isr &= ~(1<< ((val-0x60)));
				        pic.isrr = ~pic.isr;
				        pic.check_after_EOI();
        //				if (val&0x80);	// perform rotation
                    } else {		// nonspecific EOI
                        if (pic.active_irq != 8) {
                            //If there is no irq in service, ignore the call, some games send an eoi to both pics when a sound irq happens (regardless of the irq).
                            pic.isr &= ~(1 << (pic.active_irq));
                            pic.isrr = ~pic.isr;
                            pic.check_after_EOI();
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
            switch(pic.icw_index) {
            case 0:                        /* mask register */
                pic.set_imr(val);
                break;
            case 1:                        /* icw2          */
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIC,LogSeverities.LOG_NORMAL,(port==0x21 ? 0 : 1)+":Base vector "+Integer.toString(val,16));
                pic.vector_base = val&0xf8;
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
            if (pic.request_issr){
                return pic.isr;
            } else {
                return pic.irr;
            }
        }
    };

    static private IoHandler.IO_ReadHandler read_data = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            PIC_Controller pic=pics[port==0x21 ? 0 : 1];
	        return pic.imr;
        }
    };

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
		PIC_Ticks=0;
		/*Bitu*/int i;
        for (i=0;i<ReadHandler.length;i++)
            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        for (i=0;i<WriteHandler.length;i++)
            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
		for (i=0;i<2;i++) {
			pics[i].auto_eoi=false;
			pics[i].rotate_on_auto_eoi=false;
			pics[i].request_issr=false;
			pics[i].special=false;
			pics[i].single=false;
			pics[i].icw_index=0;
			pics[i].icw_words=0;
            pics[i].irr = pics[i].isr = pics[i].imrr = 0;
			pics[i].isrr = pics[i].imr = 0xff;
			pics[i].active_irq = 8;
		}
        master.vector_base = 0x08;
        slave.vector_base = 0x70;

        PIC_SetIRQMask(0,false);					/* Enable system timer */
        PIC_SetIRQMask(1,false);					/* Enable system timer */
        PIC_SetIRQMask(2,false);					/* Enable second pic */
        PIC_SetIRQMask(8,false);					/* Enable RTC IRQ */

		if (Dosbox.machine==MachineType.MCH_PCJR) {
			/* Enable IRQ6 (replacement for the NMI for PCJr) */
			PIC_SetIRQMask(6,false);
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
            for (int i=0;i<pics.length;i++)
                pics[i] = null;
            master = null;
            slave = null;
            pic_queue = null;
        }
    };
    
    public static Section.SectionFunction PIC_Init = new Section.SectionFunction() {
        public void call(Section section) {
            pic_queue = new Pic_queue();
            for (int i=0;i<pics.length;i++)
                pics[i] = new PIC_Controller();
            master = pics[0];
            slave = pics[1];
            test = new Pic(section);
            if (section!=null)
                section.AddDestroyFunction(PIC_Destroy);
        }
    };
}
