package jdos.hardware;

import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.ShortPtr;

public class PCSpeaker extends Module_base {
    private static final double PI = 3.14159265358979323846;

    private static final int SPKR_ENTRIES = 1024;
    private static final int SPKR_VOLUME = 5000;
    private static final float SPKR_SPEED = (float)((SPKR_VOLUME*2)/0.070f);

    private static final class SPKR_MODES {
        public static final int SPKR_OFF=0;
        public static final int SPKR_ON=1;
        public static final int SPKR_PIT_OFF=2;
        public static final int SPKR_PIT_ON=3;
    }

    private static class DelayEntry {
        float index;
        float vol;
    }

    static private class Spkr {
        public Spkr() {
            for (int i=0;i<entries.length;i++)
                entries[i] = new DelayEntry();
        }
        Mixer.MixerChannel chan = new Mixer.MixerChannel();
        int mode;
        /*Bitu*/int pit_mode;
        /*Bitu*/int rate;

        float pit_last;
        float pit_new_max,pit_new_half;
        float pit_max,pit_half;
        float pit_index;
        float volwant,volcur;
        /*Bitu*/int last_ticks;
        float last_index;
        /*Bitu*/int min_tr;
        DelayEntry[] entries = new DelayEntry[SPKR_ENTRIES];
        /*Bitu*/int used;
    }

    static private Spkr spkr;

    static private void AddDelayEntry(float index,float vol) {
        if (spkr.used==SPKR_ENTRIES) {
            return;
        }
        spkr.entries[spkr.used].index=index;
        spkr.entries[spkr.used].vol=vol;
        spkr.used++;
    }


    static private void ForwardPIT(float newindex) {
        float passed=(newindex-spkr.last_index);
        float delay_base=spkr.last_index;
        spkr.last_index=newindex;
        switch (spkr.pit_mode) {
        case 0:
            return;
        case 1:
            return;
        case 2:
            while (passed>0) {
                /* passed the initial low cycle? */
                if (spkr.pit_index>=spkr.pit_half) {
                    /* Start a new low cycle */
                    if ((spkr.pit_index+passed)>=spkr.pit_max) {
                        float delay=spkr.pit_max-spkr.pit_index;
                        delay_base+=delay;passed-=delay;
                        spkr.pit_last=-SPKR_VOLUME;
                        if (spkr.mode==SPKR_MODES.SPKR_PIT_ON) AddDelayEntry(delay_base,spkr.pit_last);
                        spkr.pit_index=0;
                    } else {
                        spkr.pit_index+=passed;
                        return;
                    }
                } else {
                    if ((spkr.pit_index+passed)>=spkr.pit_half) {
                        float delay=spkr.pit_half-spkr.pit_index;
                        delay_base+=delay;passed-=delay;
                        spkr.pit_last=SPKR_VOLUME;
                        if (spkr.mode==SPKR_MODES.SPKR_PIT_ON) AddDelayEntry(delay_base,spkr.pit_last);
                        spkr.pit_index=spkr.pit_half;
                    } else {
                        spkr.pit_index+=passed;
                        return;
                    }
                }
            }
            break;
            //END CASE 2
        case 3:
            while (passed>0) {
                /* Determine where in the wave we're located */
                if (spkr.pit_index>=spkr.pit_half) {
                    if ((spkr.pit_index+passed)>=spkr.pit_max) {
                        float delay=spkr.pit_max-spkr.pit_index;
                        delay_base+=delay;passed-=delay;
                        spkr.pit_last=SPKR_VOLUME;
                        if (spkr.mode==SPKR_MODES.SPKR_PIT_ON) AddDelayEntry(delay_base,spkr.pit_last);
                        spkr.pit_index=0;
                        /* Load the new count */
                        spkr.pit_half=spkr.pit_new_half;
                        spkr.pit_max=spkr.pit_new_max;
                    } else {
                        spkr.pit_index+=passed;
                        return;
                    }
                } else {
                    if ((spkr.pit_index+passed)>=spkr.pit_half) {
                        float delay=spkr.pit_half-spkr.pit_index;
                        delay_base+=delay;passed-=delay;
                        spkr.pit_last=-SPKR_VOLUME;
                        if (spkr.mode==SPKR_MODES.SPKR_PIT_ON) AddDelayEntry(delay_base,spkr.pit_last);
                        spkr.pit_index=spkr.pit_half;
                        /* Load the new count */
                        spkr.pit_half=spkr.pit_new_half;
                        spkr.pit_max=spkr.pit_new_max;
                    } else {
                        spkr.pit_index+=passed;
                        return;
                    }
                }
            }
            break;
            //END CASE 3
        case 4:
            if (spkr.pit_index<spkr.pit_max) {
                /* Check if we're gonna pass the end this block */
                if (spkr.pit_index+passed>=spkr.pit_max) {
                    float delay=spkr.pit_max-spkr.pit_index;
                    delay_base+=delay;passed-=delay;
                    spkr.pit_last=-SPKR_VOLUME;
                    if (spkr.mode==SPKR_MODES.SPKR_PIT_ON) AddDelayEntry(delay_base,spkr.pit_last);				//No new events unless reprogrammed
                    spkr.pit_index=spkr.pit_max;
                } else spkr.pit_index+=passed;
            }
            break;
            //END CASE 4
        }
    }

    public static void PCSPEAKER_SetCounter(/*Bitu*/int cntr,/*Bitu*/int mode) {
        if (spkr.last_ticks==0) {
            if(spkr.chan!=null) spkr.chan.Enable(true);
            spkr.last_index=0;
        }
        spkr.last_ticks=Pic.PIC_Ticks;
        float newindex=Pic.PIC_TickIndex();
        ForwardPIT(newindex);
        switch (mode) {
        case 0:		/* Mode 0 one shot, used with realsound */
            if (spkr.mode!=SPKR_MODES.SPKR_PIT_ON) return;
            if (cntr>80) {
                cntr=80;
            }
            spkr.pit_last=((float)cntr-40)*(SPKR_VOLUME/40.0f);
            AddDelayEntry(newindex,spkr.pit_last);
            spkr.pit_index=0;
            break;
        case 1:
            if (spkr.mode!=SPKR_MODES.SPKR_PIT_ON) return;
            spkr.pit_last=SPKR_VOLUME;
            AddDelayEntry(newindex,spkr.pit_last);
            break;
        case 2:			/* Single cycle low, rest low high generator */
            spkr.pit_index=0;
            spkr.pit_last=-SPKR_VOLUME;
            AddDelayEntry(newindex,spkr.pit_last);
            spkr.pit_half=(1000.0f/Timer.PIT_TICK_RATE)*1;
            spkr.pit_max=(1000.0f/Timer.PIT_TICK_RATE)*cntr;
            break;
        case 3:		/* Square wave generator */
            if (cntr<spkr.min_tr) {
                /* skip frequencies that can't be represented */
                spkr.pit_last=0;
                spkr.pit_mode=0;
                return;
            }
            spkr.pit_new_max=(1000.0f/Timer.PIT_TICK_RATE)*cntr;
            spkr.pit_new_half=spkr.pit_new_max/2;
            break;
        case 4:		/* Software triggered strobe */
            spkr.pit_last=SPKR_VOLUME;
            AddDelayEntry(newindex,spkr.pit_last);
            spkr.pit_index=0;
            spkr.pit_max=(1000.0f/Timer.PIT_TICK_RATE)*cntr;
            break;
        default:
            if (Config.C_DEBUG)
                Log.log_msg("Unhandled speaker mode "+mode);
            return;
        }
        spkr.pit_mode=mode;
    }

    public static void PCSPEAKER_SetType(/*Bitu*/int mode) {
        if (spkr.last_ticks==0) {
            if(spkr.chan!=null) spkr.chan.Enable(true);
            spkr.last_index=0;
        }
        spkr.last_ticks=Pic.PIC_Ticks;
        float newindex=Pic.PIC_TickIndex();
        ForwardPIT(newindex);
        switch (mode) {
        case 0:
            spkr.mode=SPKR_MODES.SPKR_OFF;
            AddDelayEntry(newindex,-SPKR_VOLUME);
            break;
        case 1:
            spkr.mode=SPKR_MODES.SPKR_PIT_OFF;
            AddDelayEntry(newindex,-SPKR_VOLUME);
            break;
        case 2:
            spkr.mode=SPKR_MODES.SPKR_ON;
            AddDelayEntry(newindex,SPKR_VOLUME);
            break;
        case 3:
            if (spkr.mode!=SPKR_MODES.SPKR_PIT_ON) {
                AddDelayEntry(newindex,spkr.pit_last);
            }
            spkr.mode=SPKR_MODES.SPKR_PIT_ON;
            break;
        }
    }

    static private Mixer.MIXER_Handler PCSPEAKER_CallBack = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int len) {
            /*Bit16s*/short[] stream=new short[len];
            ForwardPIT(1);
            spkr.last_index=0;
            /*Bitu*/int count=len;
            /*Bitu*/int pos=0;
            float sample_base=0;
            float sample_add=(1.0001f)/len;
            int i=0;
            while (count--!=0) {
                float index=sample_base;
                sample_base+=sample_add;
                float end=sample_base;
                double value=0;
                while(index<end) {
                    /* Check if there is an upcoming event */
                    if (spkr.used!=0 && spkr.entries[pos].index<=index) {
                        spkr.volwant=spkr.entries[pos].vol;
                        pos++;spkr.used--;
                        continue;
                    }
                    float vol_end;
                    if (spkr.used!=0 && spkr.entries[pos].index<end) {
                        vol_end=spkr.entries[pos].index;
                    } else vol_end=end;
                    float vol_len=vol_end-index;
                    /* Check if we have to slide the volume */
                    float vol_diff=spkr.volwant-spkr.volcur;
                    if (vol_diff == 0) {
                        value+=spkr.volcur*vol_len;
                        index+=vol_len;
                    } else {
                        /* Check how long it will take to goto new level */
                        float vol_time=Math.abs(vol_diff)/SPKR_SPEED;
                        if (vol_time<=vol_len) {
                            /* Volume reaches endpoint in this block, calc until that point */
                            value+=vol_time*spkr.volcur;
                            value+=vol_time*vol_diff/2;
                            index+=vol_time;
                            spkr.volcur=spkr.volwant;
                        } else {
                            /* Volume still not reached in this block */
                            value+=spkr.volcur*vol_len;
                            if (vol_diff<0) {
                                value-=(SPKR_SPEED*vol_len*vol_len)/2;
                                spkr.volcur-=SPKR_SPEED*vol_len;
                            } else {
                                value+=(SPKR_SPEED*vol_len*vol_len)/2;
                                spkr.volcur+=SPKR_SPEED*vol_len;
                            }
                            index+=vol_len;
                        }
                    }
                }
                stream[i++]=(/*Bit16s*/short)(value/sample_add);
            }
            if(spkr.chan!=null) spkr.chan.AddSamples_m16(len,stream);

            //Turn off speaker after 10 seconds of idle or one second idle when in off mode
            boolean turnoff = false;
            /*Bitu*/int test_ticks = Pic.PIC_Ticks;
            if ((spkr.last_ticks + 10000) < test_ticks) turnoff = true;
            if((spkr.mode == SPKR_MODES.SPKR_OFF) && ((spkr.last_ticks + 1000) < test_ticks)) turnoff = true;

            if(turnoff){
                if(spkr.volwant == 0) {
                    spkr.last_ticks = 0;
                    if(spkr.chan!=null) spkr.chan.Enable(false);
                } else {
                    if(spkr.volwant > 0) spkr.volwant--; else spkr.volwant++;

                }
            }

        }
    };
    private Mixer.MixerObject MixerChan = new Mixer.MixerObject();
    public PCSpeaker(Section configuration) {
        super(configuration);
        spkr.chan=null;
		Section_prop section=(Section_prop)configuration;
		if(!section.Get_bool("pcspeaker")) return;
		spkr.mode=SPKR_MODES.SPKR_OFF;
		spkr.last_ticks=0;
		spkr.last_index=0;
		spkr.rate=section.Get_int("pcrate");
		spkr.pit_max=(1000.0f/Timer.PIT_TICK_RATE)*65535;
		spkr.pit_half=spkr.pit_max/2;
		spkr.pit_new_max=spkr.pit_max;
		spkr.pit_new_half=spkr.pit_half;
		spkr.pit_index=0;
		spkr.min_tr=(Timer.PIT_TICK_RATE+spkr.rate/2-1)/(spkr.rate/2);
		spkr.used=0;
		/* Register the sound channel */
		spkr.chan=MixerChan.Install(PCSPEAKER_CallBack,spkr.rate,"SPKR");
    }

    static private PCSpeaker test;
    private static Section.SectionFunction PCSPEAKER_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
            spkr = null;
        }
    };

    public static Section.SectionFunction PCSPEAKER_Init = new Section.SectionFunction() {
        public void call(Section section) {
            spkr = new Spkr();
            test = new PCSpeaker(section);
	        section.AddDestroyFunction(PCSPEAKER_ShutDown,true);
        }
    };
}
