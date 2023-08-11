package jdos.hardware;

import jdos.cpu.CPU;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.StringHelper;

public class Timer extends Module_base {
    static public final int PIT_TICK_RATE = 1193182;
    
    static public interface TIMER_TickHandler {
        public void call();
    }
    // FROM pic.pp
    static private class TickerBlock {
        TIMER_TickHandler handler;
        TickerBlock next;
    }
    static private TickerBlock firstticker;

    static public void TIMER_DelTickHandler(TIMER_TickHandler handler) {
	    TickerBlock ticker=firstticker;
        TickerBlock prev = null;
        while (ticker!=null) {
            if (ticker.handler==handler) {
                if (prev == null)
                    firstticker = ticker.next;
                else
                    prev.next = ticker.next;
                return;
            }
            prev=ticker;
            ticker=ticker.next;
        }
    }

    static public void TIMER_AddTickHandler(TIMER_TickHandler handler) {
        TickerBlock newticker=new TickerBlock();
        newticker.next=firstticker;
        newticker.handler=handler;
        firstticker=newticker;
    }

    static public void TIMER_AddTick() {
        /* Setup new amount of cycles for PIC */
        CPU.CPU_CycleLeft=CPU.CPU_CycleMax;
        CPU.CPU_Cycles=0;
        Pic.PIC_Ticks++;
        /* Go through the list of scheduled events and lower their index with 1000 */
        Pic.PICEntry entry=Pic.pic_queue.next_entry;
        while (entry != null) {
            entry.index -= 1.0;
            entry=entry.next;
        }
        /* Call our list of ticker handlers */
        TickerBlock ticker=firstticker;
        while (ticker!=null) {
            TickerBlock nextticker=ticker.next;
            ticker.handler.call();
            ticker=nextticker;
        }
    }
    // END FROM pic.cpp

    static public int BIN2BCD(/*Bit16u*/int val) {
        return val%10 + (((val/10)%10)<<4)+ (((val/100)%10)<<8) + (((val/1000)%10)<<12);
    }

    static public int BCD2BIN(/*Bit16u*/int val) {
	    return (val&0x0f) +((val>>4)&0x0f) *10 +((val>>8)&0x0f) *100 +((val>>12)&0x0f) *1000;
    }

    private static class PIT_Block {
        /*Bitu*/int cntr;
        float delay;
        double start;

        /*Bit16u*/int read_latch;
        /*Bit16u*/int write_latch;

        /*Bit8u*/short mode;
        /*Bit8u*/short latch_mode;
        /*Bit8u*/short read_state;
        /*Bit8u*/short write_state;

        boolean bcd;
        boolean go_read_latch;
        boolean new_mode;
        boolean counterstatus_set;
        boolean counting;
        boolean update_count;
    }

    static private PIT_Block[] pit = new PIT_Block[3];
    static private boolean gate2;

    static private /*Bit8u*/short latched_timerstatus;
    // the timer status can not be overwritten until it is read or the timer was
    // reprogrammed.
    static private boolean latched_timerstatus_locked;

    static private Pic.PIC_EventHandler PIT0_Event = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            Pic.PIC_ActivateIRQ(0);
            if (pit[0].mode != 0) {
                pit[0].start += pit[0].delay;

                if (pit[0].update_count) {
                    pit[0].delay=(1000.0f/((float)PIT_TICK_RATE/(float)pit[0].cntr));
                    pit[0].update_count=false;
                }
                Pic.PIC_AddEvent(PIT0_Event,pit[0].delay);
            }
        }
        public String toString() {
            return "PIT0_Event";
        }
    };

    static double fmod(double d, double d1) {
        return d % d1;
    }

    static boolean counter_output(/*Bitu*/int counter) {
        PIT_Block p=pit[counter];
        double index=Pic.PIC_FullIndex()-p.start;
        switch (p.mode) {
        case 0:
            if (p.new_mode) return false;
            if (index>p.delay) return true;
            else return false;
        case 2:
            if (p.new_mode) return true;
            index=fmod(index,(double)p.delay);
            return index>0;
        case 3:
            if (p.new_mode) return true;
            index=fmod(index,(double)p.delay);
            return index*2<p.delay;
        case 4:
            //Only low on terminal count
            // if(fmod(index,(double)p.delay) == 0) return false; //Maybe take one rate tick in consideration
            //Easiest solution is to report always high (Space marines uses this mode)
            return true;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_PIT, LogSeverities.LOG_ERROR,"Illegal Mode "+p.mode+" for reading output");
            return true;
        }
    }
    static private void status_latch(/*Bitu*/int counter) {
        // the timer status can not be overwritten until it is read or the timer was
        // reprogrammed.
        if(!latched_timerstatus_locked)	{
            PIT_Block p=pit[counter];
            latched_timerstatus=0;
            // Timer Status Word
            // 0: BCD
            // 1-3: Timer mode
            // 4-5: read/load mode
            // 6: "NULL" - this is 0 if "the counter value is in the counter" ;)
            // should rarely be 1 (i.e. on exotic modes)
            // 7: OUT - the logic level on the Timer output pin
            if(p.bcd)latched_timerstatus|=0x1;
            latched_timerstatus|=((p.mode&7)<<1);
            if((p.read_state==0)||(p.read_state==3)) latched_timerstatus|=0x30;
            else if(p.read_state==1) latched_timerstatus|=0x10;
            else if(p.read_state==2) latched_timerstatus|=0x20;
            if(counter_output(counter)) latched_timerstatus|=0x80;
            if(p.new_mode) latched_timerstatus|=0x40;
            // The first thing that is being read from this counter now is the
            // counter status.
            p.counterstatus_set=true;
            latched_timerstatus_locked=true;
        }
    }

    static void counter_latch(/*Bitu*/int counter) {
        /* Fill the read_latch of the selected counter with current count */
        PIT_Block p=pit[counter];
        p.go_read_latch=false;

        //If gate2 is disabled don't update the read_latch
        if(counter == 2 && !gate2 && p.mode !=1) return;
        if (p.new_mode) {
            double passed_time = Pic.PIC_FullIndex() - p.start;
            /*Bitu*/int ticks_since_then = (/*Bitu*/int)(passed_time / (1000.0/PIT_TICK_RATE));
            //if (p->mode==3) ticks_since_then /= 2; // TODO figure this out on real hardware
            p.read_latch -= ticks_since_then;
            return;
        }
        double index=Pic.PIC_FullIndex()-p.start;
        switch (p.mode) {
        case 4:		/* Software Triggered Strobe */
        case 0:		/* Interrupt on Terminal Count */
            /* Counter keeps on counting after passing terminal count */
            if (index>p.delay) {
                index-=p.delay;
                if(p.bcd) {
                    index = fmod(index,(1000.0/PIT_TICK_RATE)*10000.0);
                    p.read_latch = (/*Bit16u*/int)(9999-index*(PIT_TICK_RATE/1000.0));
                } else {
                    index = fmod(index,(1000.0/PIT_TICK_RATE)*(double)0x10000);
                    p.read_latch = (/*Bit16u*/int)(0xffff-index*(PIT_TICK_RATE/1000.0));
                }
            } else {
                p.read_latch=(/*Bit16u*/int)(p.cntr-index*(PIT_TICK_RATE/1000.0));
            }
            break;
        case 1: // countdown
            if(p.counting) {
                if (index>p.delay) { // has timed out
                    p.read_latch = 0xffff; //unconfirmed
                } else {
                    p.read_latch=(/*Bit16u*/int)(p.cntr-index*(PIT_TICK_RATE/1000.0));
                }
            }
            break;
        case 2:		/* Rate Generator */
            index=fmod(index,(double)p.delay);
            p.read_latch=(/*Bit16u*/int)(p.cntr - (index/p.delay)*p.cntr);
            break;
        case 3:		/* Square Wave Rate Generator */
            index=fmod(index,(double)p.delay);
            index*=2;
            if (index>p.delay) index-=p.delay;
            p.read_latch=(/*Bit16u*/int)(p.cntr - (index/p.delay)*p.cntr);
            // In mode 3 it never returns odd numbers LSB (if odd number is written 1 will be
            // subtracted on first clock and then always 2)
            // fixes "Corncob 3D"
            p.read_latch&=0xfffe;
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_ERROR,"Illegal Mode "+p.mode+" for reading counter "+counter);
            p.read_latch=0xffff;
            break;
        }
    }


    static private IoHandler.IO_WriteHandler write_latch = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
        //Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_ERROR,"port %X write:%X state:%X",port,val,pit[port-0x40].write_state);
            /*Bitu*/int counter=port-0x40;
            PIT_Block p=pit[counter];
            if(p.bcd == true) BIN2BCD(p.write_latch);

            switch (p.write_state) {
                case 0:
                    p.write_latch = p.write_latch | ((val & 0xff) << 8);
                    p.write_state = 3;
                    break;
                case 3:
                    p.write_latch = val & 0xff;
                    p.write_state = 0;
                    break;
                case 1:
                    p.write_latch = val & 0xff;
                    break;
                case 2:
                    p.write_latch = (val & 0xff) << 8;
                break;
            }
            if (p.bcd==true) BCD2BIN(p.write_latch);
            if (p.write_state != 0) {
                if (p.write_latch == 0) {
                    if (p.bcd == false) p.cntr = 0x10000;
                    else p.cntr=9999;
                } else p.cntr = p.write_latch;

                if ((!p.new_mode) && (p.mode == 2) && (counter == 0)) {
                    // In mode 2 writing another value has no direct effect on the count
                    // until the old one has run out. This might apply to other modes too.
                    // This is not fixed for PIT2 yet!!
                    p.update_count=true;
                    return;
                }
                p.start=Pic.PIC_FullIndex();
                p.delay=(1000.0f/((float)PIT_TICK_RATE/(float)p.cntr));

                switch (counter) {
                case 0x00:			/* Timer hooked to IRQ 0 */
                    if (p.new_mode || p.mode == 0 ) {
                        if(p.mode==0) Pic.PIC_RemoveEvents(PIT0_Event); // DoWhackaDo demo
                        Pic.PIC_AddEvent(PIT0_Event,p.delay);
                    } else Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_NORMAL,"PIT 0 Timer set without new control word");
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_NORMAL,"PIT 0 Timer at "+ StringHelper.format(1000.0/p.delay, 4)+" Hz mode "+p.mode);
                    break;
                case 0x02:			/* Timer hooked to PC-Speaker */
        //			LOG(LOG_PIT,"PIT 2 Timer at %.3g Hz mode %d",PIT_TICK_RATE/(double)p.cntr,p.mode);
                    PCSpeaker.PCSPEAKER_SetCounter(p.cntr,p.mode);
                    break;
                default:
                    Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_ERROR,"PIT:Illegal timer selected for writing");
                }
                p.new_mode=false;
            }
        }
    };

    static private IoHandler.IO_ReadHandler read_latch = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
        //Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_ERROR,"port read %X",port);
            /*Bit32u*/int counter=port-0x40;
            /*Bit8u*/int ret=0;
            if(pit[counter].counterstatus_set){
                pit[counter].counterstatus_set = false;
                latched_timerstatus_locked = false;
                ret = latched_timerstatus;
            } else {
                if (pit[counter].go_read_latch == true)
                    counter_latch(counter);

                if( pit[counter].bcd == true) BIN2BCD(pit[counter].read_latch);

                switch (pit[counter].read_state) {
                case 0: /* read MSB & return to state 3 */
                    ret=(pit[counter].read_latch >> 8) & 0xff;
                    pit[counter].read_state = 3;
                    pit[counter].go_read_latch = true;
                    break;
                case 3: /* read LSB followed by MSB */
                    ret = pit[counter].read_latch & 0xff;
                    pit[counter].read_state = 0;
                    break;
                case 1: /* read LSB */
                    ret = pit[counter].read_latch & 0xff;
                    pit[counter].go_read_latch = true;
                    break;
                case 2: /* read MSB */
                    ret = (pit[counter].read_latch >> 8) & 0xff;
                    pit[counter].go_read_latch = true;
                    break;
                default:
                    Log.exit("Timer.cpp: error in readlatch");
                    break;
                }
                if( pit[counter].bcd == true) BCD2BIN(pit[counter].read_latch);
            }
            return ret;
        }
    };

    static private IoHandler.IO_WriteHandler write_p43 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
        //Log.log(LogTypes.LOG_PIT,LogSeverities.LOG_ERROR,"port 43 %X",val);
            /*Bitu*/int latch=(val >> 6) & 0x03;
            switch (latch) {
            case 0:
            case 1:
            case 2:
                if ((val & 0x30) == 0) {
                    /* Counter latch command */
                    counter_latch(latch);
                } else {
                    // save output status to be used with timer 0 irq
			        boolean old_output = counter_output(0);
			        // save the current count value to be re-used in undocumented newmode
			        counter_latch(latch);
                    pit[latch].bcd = (val&1)>0;
                    if ((val & 1)!=0) {
                        if(pit[latch].cntr>=9999) pit[latch].cntr=9999;
                    }

                    // Timer is being reprogrammed, unlock the status
                    if(pit[latch].counterstatus_set) {
                        pit[latch].counterstatus_set=false;
                        latched_timerstatus_locked=false;
                    }
                    pit[latch].start = Pic.PIC_FullIndex(); // for undocumented newmode
			        pit[latch].go_read_latch = true;
                    pit[latch].update_count = false;
                    pit[latch].counting = false;
                    pit[latch].read_state  = (short)((val >> 4) & 0x03);
                    pit[latch].write_state = (short)((val >> 4) & 0x03);
                    /*Bit8u*/short mode    = (short)((val >> 1) & 0x07);
                    if (mode > 5)
                        mode -= 4; //6,7 become 2 and 3

                    pit[latch].mode = mode;

                    /* If the line goes from low to up => generate irq.
                     *      ( BUT needs to stay up until acknowlegded by the cpu!!! therefore: )
                     * If the line goes to low => disable irq.
                     * Mode 0 starts with a low line. (so always disable irq)
                     * Mode 2,3 start with a high line.
                     * counter_output tells if the current counter is high or low
                     * So actually a mode 3 timer enables and disables irq al the time. (not handled) */

                    if (latch == 0) {
                        Pic.PIC_RemoveEvents(PIT0_Event);
                        if((mode != 0)&& !old_output) {
                            Pic.PIC_ActivateIRQ(0);
                        } else {
                            Pic.PIC_DeActivateIRQ(0);
                        }
                    }
                    pit[latch].new_mode = true;
                }
                break;
            case 3:
                if ((val & 0x20)==0) {	/* Latch multiple pit counters */
                    if ((val & 0x02)!=0) counter_latch(0);
                    if ((val & 0x04)!=0) counter_latch(1);
                    if ((val & 0x08)!=0) counter_latch(2);
                }
                // status and values can be latched simultaneously
                if ((val & 0x10)==0) {	/* Latch status words */
                    // but only 1 status can be latched simultaneously
                    if ((val & 0x02)!=0) status_latch(0);
                    else if ((val & 0x04)!=0) status_latch(1);
                    else if ((val & 0x08)!=0) status_latch(2);
                }
                break;
            }
        }
    };

    static public void TIMER_SetGate2(boolean in) {
        //No changes if gate doesn't change
        if(gate2 == in) return;

        switch(pit[2].mode) {
        case 0:
            if(in) pit[2].start = Pic.PIC_FullIndex();
            else {
                //Fill readlatch and store it.
                counter_latch(2);
                pit[2].cntr = pit[2].read_latch;
            }
            break;
        case 1:
            // gate 1 on: reload counter; off: nothing
            if(in) {
                pit[2].counting = true;
                pit[2].start = Pic.PIC_FullIndex();
            }
            break;
        case 2:
        case 3:
            //If gate is enabled restart counting. If disable store the current read_latch
            if(in) pit[2].start = Pic.PIC_FullIndex();
            else counter_latch(2);
            break;
        case 4:
        case 5:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_MISC,LogSeverities.LOG_WARN,"unsupported gate 2 mode "+Integer.toString(pit[2].mode,16));
            break;
        }
        gate2 = in; //Set it here so the counter_latch above works
    }

	private IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[4];
	private IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[4];
    public Timer(Section configuration) {
        super(configuration);
        for (int i=0;i<ReadHandler.length;i++)
            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        for (int i=0;i<WriteHandler.length;i++)
            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();

        WriteHandler[0].Install(0x40,write_latch,IoHandler.IO_MB);
	//	WriteHandler[1].Install(0x41,write_latch,IO_MB);
		WriteHandler[2].Install(0x42,write_latch,IoHandler.IO_MB);
		WriteHandler[3].Install(0x43,write_p43,IoHandler.IO_MB);
		ReadHandler[0].Install(0x40,read_latch,IoHandler.IO_MB);
		ReadHandler[1].Install(0x41,read_latch,IoHandler.IO_MB);
		ReadHandler[2].Install(0x42,read_latch,IoHandler.IO_MB);
		/* Setup Timer 0 */
		pit[0].cntr=0x10000;
		pit[0].write_state = 3;
		pit[0].read_state = 3;
		pit[0].read_latch=0;
		pit[0].write_latch=0;
		pit[0].mode=3;
		pit[0].bcd = false;
		pit[0].go_read_latch = true;
		pit[0].counterstatus_set = false;
		pit[0].update_count = false;

		pit[1].bcd = false;
		pit[1].write_state = 1;
		pit[1].read_state = 1;
		pit[1].go_read_latch = true;
		pit[1].cntr = 18;
		pit[1].mode = 2;
		pit[1].write_state = 3;
		pit[1].counterstatus_set = false;

		pit[2].read_latch=1320;	/* MadTv1 */
		pit[2].write_state = 3; /* Chuck Yeager */
		pit[2].read_state = 3;
		pit[2].mode=3;
		pit[2].bcd=false;
		pit[2].cntr=1320;
		pit[2].go_read_latch=true;
		pit[2].counterstatus_set = false;
		pit[2].counting = false;

		pit[0].delay=(1000.0f/((float)PIT_TICK_RATE/(float)pit[0].cntr));
		pit[1].delay=(1000.0f/((float)PIT_TICK_RATE/(float)pit[1].cntr));
		pit[2].delay=(1000.0f/((float)PIT_TICK_RATE/(float)pit[2].cntr));

		latched_timerstatus_locked=false;
		gate2 = false;
		Pic.PIC_AddEvent(PIT0_Event,pit[0].delay);
    }

    private static Timer test;

    public static Section.SectionFunction TIMER_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            Pic.PIC_RemoveEvents(PIT0_Event);
            test = null;
            for (int i=0;i<pit.length;i++)
                pit[i] = null;
            firstticker = null;
        }
    };
    public static Section.SectionFunction TIMER_Init = new Section.SectionFunction() {
        public void call(Section section) {
            firstticker = null;
            for (int i=0;i<pit.length;i++)
                pit[i] = new PIT_Block();
            test = new Timer(section);
            section.AddDestroyFunction(TIMER_Destroy);
        }
    };
}
