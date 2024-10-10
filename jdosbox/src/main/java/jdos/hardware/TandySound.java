package jdos.hardware;

import jdos.Dosbox;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.IntRef;

public class TandySound extends Module_base {
    private static final int MAX_OUTPUT = 0x7fff;
    private static final int STEP = 0x10000;
    
    /* Formulas for noise generator */
    /* bit0 = output */
    
    /* noise feedback for white noise mode (verified on real SN76489 by John Kortink) */
    private static final int FB_WNOISE = 0x14002;	/* (16bits) bit16 = bit0(out) ^ bit2 ^ bit15 */
    
    /* noise feedback for periodic noise mode */
    //#define FB_PNOISE 0x10000 /* 16bit rorate */
    private static final int FB_PNOISE = 0x08000;   /* JH 981127 - fixes Do Run Run */
    
    /*
    0x08000 is definitely wrong. The Master System conversion of Marble Madness
    uses periodic noise as a baseline. With a 15-bit rotate, the bassline is
    out of tune.
    The 16-bit rotate has been confirmed against a real PAL Sega Master System 2.
    Hope that helps the System E stuff, more news on the PSG as and when!
    */
    
    /* noise generator start preset (for periodic noise) */
    private static final int NG_PRESET = 0x0f35;
    
    
    final private static class SN76496 {
        int SampleRate;
        int UpdateStep;
        int[] VolTable = new int[16];	/* volume table         */
        int[] Register = new int[8];	/* registers */
        int LastRegister;	/* last register written */
        int[] Volume = new int[4];		/* volume of voice 0-2 and noise */
        int RNG;		/* noise generator      */
        int NoiseFB;		/* noise feedback mask */
        int[] Period = new int[4];
        int[] Count = new int[4];
        int[] Output = new int[4];
    }
    
    static private SN76496 sn = new SN76496();
    
    private static final int TDAC_DMA_BUFSIZE = 1024;
    
    final static private class Tandy {
        Mixer.MixerChannel chan;
        boolean enabled;
        /*Bitu*/int last_write;
        static public class Dac {
            Mixer.MixerChannel chan;
            boolean enabled;
            static public class HW {
                /*Bitu*/int base;
                /*Bit8u*/short irq,dma;
            }
            HW hw = new HW();
            public static class DMA {
                /*Bitu*/int rate;
                /*Bit8u*/byte[] buf = new byte[TDAC_DMA_BUFSIZE];
                /*Bit8u*/byte[] last_sample = new byte[1];
                jdos.hardware.DMA.DmaChannel chan;
                boolean transfer_done;
            }
            DMA dma = new DMA();
            /*Bit8u*/short mode,control;
            /*Bit16u*/int frequency;
            /*Bit8u*/short amplitude;
            boolean irq_activated;
        }
        Dac dac = new Dac();
    }
    final static private Tandy tandy = new Tandy();
    
    
    private static final IoHandler.IO_WriteHandler SN76496Write = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int data, /*Bitu*/int iolen) {
            SN76496 R = sn;

            tandy.last_write=Pic.PIC_Ticks;
            if (!tandy.enabled) {
                tandy.chan.Enable(true);
                tandy.enabled=true;
            }

            /* update the output buffer before changing the registers */

            if ((data & 0x80)!=0) {
                int r = (data & 0x70) >> 4;
                int c = r/2;

                R.LastRegister = r;
                R.Register[r] = (R.Register[r] & 0x3f0) | (data & 0x0f);
                switch (r)
                {
                    case 0:	/* tone 0 : frequency */
                    case 2:	/* tone 1 : frequency */
                    case 4:	/* tone 2 : frequency */
                        R.Period[c] = R.UpdateStep * R.Register[r];
                        if (R.Period[c] == 0) R.Period[c] = 0x3fe;
                        if (r == 4)
                        {
                            /* update noise shift frequency */
                            if ((R.Register[6] & 0x03) == 0x03)
                                R.Period[3] = 2 * R.Period[2];
                        }
                        break;
                    case 1:	/* tone 0 : volume */
                    case 3:	/* tone 1 : volume */
                    case 5:	/* tone 2 : volume */
                    case 7:	/* noise  : volume */
                        R.Volume[c] = R.VolTable[data & 0x0f];
                        break;
                    case 6:	/* noise  : frequency, mode */
                        {
                            int n = R.Register[6];
                            R.NoiseFB = (n & 4)!=0 ? FB_WNOISE : FB_PNOISE;
                            n &= 3;
                            /* N/512,N/1024,N/2048,Tone #3 output */
                            R.Period[3] = (n == 3) ? 2 * R.Period[2] : (R.UpdateStep << (5+n));

                            /* reset noise shifter */
        //					R.RNG = NG_PRESET;
        //					R.Output[3] = R.RNG & 1;
                        }
                        break;
                }
            }
            else
            {
                int r = R.LastRegister;
                int c = r/2;

                switch (r)
                {
                    case 0:	/* tone 0 : frequency */
                    case 2:	/* tone 1 : frequency */
                    case 4:	/* tone 2 : frequency */
                        R.Register[r] = (R.Register[r] & 0x0f) | ((data & 0x3f) << 4);
                        R.Period[c] = R.UpdateStep * R.Register[r];
                        if (R.Period[c] == 0) R.Period[c] = 0x3fe;
                        if (r == 4)
                        {
                            /* update noise shift frequency */
                            if ((R.Register[6] & 0x03) == 0x03)
                                R.Period[3] = 2 * R.Period[2];
                        }
                        break;
                }
            }
        }
    };

    private static final Mixer.MIXER_Handler SN76496Update = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int length) {
            if ((tandy.last_write+5000)<Pic.PIC_Ticks) {
                tandy.enabled=false;
                tandy.chan.Enable(false);
            }
            int i;
            SN76496 R = sn;
            /*Bit16s*/short[] buffer=Mixer.MixTemp16;
            int bufferIndex = 0;

            /* If the volume is 0, increase the counter */
            for (i = 0;i < 4;i++)
            {
                if (R.Volume[i] == 0)
                {
                    /* note that I do count += length, NOT count = length + 1. You might think */
                    /* it's the same since the volume is 0, but doing the latter could cause */
                    /* interferencies when the program is rapidly modulating the volume. */
                    if (R.Count[i] <= (int)length*STEP) R.Count[i] += length*STEP;
                }
            }

            /*Bitu*/int count=length;
            int[] vol = new int[4];
            while (count!=0)
            {
                int out;
                int left;


                /* vol[] keeps track of how long each square wave stays */
                /* in the 1 position during the sample period. */
                vol[0] = vol[1] = vol[2] = vol[3] = 0;

                for (i = 0;i < 3;i++)
                {
                    if (R.Output[i]!=0) vol[i] += R.Count[i];
                    R.Count[i] -= STEP;
                    /* Period[i] is the half period of the square wave. Here, in each */
                    /* loop I add Period[i] twice, so that at the end of the loop the */
                    /* square wave is in the same status (0 or 1) it was at the start. */
                    /* vol[i] is also incremented by Period[i], since the wave has been 1 */
                    /* exactly half of the time, regardless of the initial position. */
                    /* If we exit the loop in the middle, Output[i] has to be inverted */
                    /* and vol[i] incremented only if the exit status of the square */
                    /* wave is 1. */
                    while (R.Count[i] <= 0)
                    {
                        R.Count[i] += R.Period[i];
                        if (R.Count[i] > 0)
                        {
                            R.Output[i] ^= 1;
                            if (R.Output[i]!=0) vol[i] += R.Period[i];
                            break;
                        }
                        R.Count[i] += R.Period[i];
                        vol[i] += R.Period[i];
                    }
                    if (R.Output[i]!=0) vol[i] -= R.Count[i];
                }

                left = STEP;
                do
                {
                    int nextevent;


                    if (R.Count[3] < left) nextevent = R.Count[3];
                    else nextevent = left;

                    if (R.Output[3]!=0) vol[3] += R.Count[3];
                    R.Count[3] -= nextevent;
                    if (R.Count[3] <= 0)
                    {
                        if ((R.RNG & 1)!=0) R.RNG ^= R.NoiseFB;
                        R.RNG >>= 1;
                        R.Output[3] = R.RNG & 1;
                        R.Count[3] += R.Period[3];
                        if (R.Output[3]!=0) vol[3] += R.Period[3];
                    }
                    if (R.Output[3]!=0) vol[3] -= R.Count[3];

                    left -= nextevent;
                } while (left > 0);

                out = vol[0] * R.Volume[0] + vol[1] * R.Volume[1] +
                        vol[2] * R.Volume[2] + vol[3] * R.Volume[3];

                if (out > MAX_OUTPUT * STEP) out = MAX_OUTPUT * STEP;

                buffer[bufferIndex++] = (/*Bit16s*/short)(out / STEP);

                count--;
            }
            tandy.chan.AddSamples_m16(length,buffer);
        }
    };

    private static void SN76496_set_clock(int clock) {
        SN76496 R = sn;
    
        /* the base clock for the tone generators is the chip clock divided by 16; */
        /* for the noise generator, it is clock / 256. */
        /* Here we calculate the number of steps which happen during one sample */
        /* at the given sample rate. No. of events = sample rate / (clock/16). */
        /* STEP is a multiplier used to turn the fraction into a fixed point */
        /* number. */
        R.UpdateStep = (int)(((double)STEP * R.SampleRate * 16) / clock);
    }
    
    
    private static void SN76496_set_gain(int gain) {
        SN76496 R = sn;
        int i;
        double out;
    
        gain &= 0xff;
    
        /* increase max output basing on gain (0.2 dB per step) */
        out = MAX_OUTPUT / 3;
        while (gain-- > 0)
            out *= 1.023292992;	/* = (10 ^ (0.2/20)) */
    
        /* build volume table (2dB per step) */
        for (i = 0;i < 15;i++)
        {
            /* limit volume to avoid clipping */
            if (out > MAX_OUTPUT / 3) R.VolTable[i] = MAX_OUTPUT / 3;
            else R.VolTable[i] = (int)out;
    
            out /= 1.258925412;	/* = 10 ^ (2/20) = 2dB */
        }
        R.VolTable[15] = 0;
    }

    private static boolean TS_Get_Address(/*Bitu*/IntRef tsaddr, /*Bitu*/IntRef tsirq, /*Bitu*/IntRef tsdma) {
        tsaddr.value=0;
        tsirq.value=0;
        tsdma.value=0;
        if (tandy.dac.enabled) {
            tsaddr.value=tandy.dac.hw.base;
            tsirq.value =tandy.dac.hw.irq;
            tsdma.value =tandy.dac.hw.dma;
            return true;
        }
        return false;
    }
    
    
    private static final DMA.DMA_CallBack TandyDAC_DMA_CallBack = new DMA.DMA_CallBack() {
        public void call(DMA.DmaChannel chan, int event) {
            if (event == DMA.DMAEvent.DMA_REACHED_TC) {
                tandy.dac.dma.transfer_done=true;
                Pic.PIC_ActivateIRQ(tandy.dac.hw.irq);
            }
        }
    };
    
    private static void TandyDACModeChanged() {
        switch (tandy.dac.mode & 3) {
        case 0:
            // joystick mode
            break;
        case 1:
            break;
        case 2:
            // recording
            break;
        case 3:
            // playback
            tandy.dac.chan.FillUp();
            if (tandy.dac.frequency!=0) {
                float freq=3579545.0f/((float)tandy.dac.frequency);
                tandy.dac.chan.SetFreq((/*Bitu*/int)freq);
                float vol=((float)tandy.dac.amplitude)/7.0f;
                tandy.dac.chan.SetVolume(vol,vol);
                if ((tandy.dac.mode&0x0c)==0x0c) {
                    tandy.dac.dma.transfer_done=false;
                    tandy.dac.dma.chan=DMA.GetDMAChannel(tandy.dac.hw.dma);
                    if (tandy.dac.dma.chan!=null) {
                        tandy.dac.dma.chan.Register_Callback(TandyDAC_DMA_CallBack);
                        tandy.dac.chan.Enable(true);
    //					LOG_MSG("Tandy DAC: playback started with freqency %f, volume %f",freq,vol);
                    }
                }
            }
            break;
        }
    }
    
    private static void TandyDACDMAEnabled() {
        TandyDACModeChanged();
    }
    
    private static void TandyDACDMADisabled() {
    }

    private static final IoHandler.IO_WriteHandler TandyDACWrite = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int data, /*Bitu*/int iolen) {
            switch (port) {
            case 0xc4: {
                /*Bitu*/int oldmode = tandy.dac.mode;
                tandy.dac.mode = (/*Bit8u*/short)(data&0xff);
                if ((data&3)!=(oldmode&3)) {
                    TandyDACModeChanged();
                }
                if (((data&0x0c)==0x0c) && ((oldmode&0x0c)!=0x0c)) {
                    TandyDACDMAEnabled();
                } else if (((data&0x0c)!=0x0c) && ((oldmode&0x0c)==0x0c)) {
                    TandyDACDMADisabled();
                }
                }
                break;
            case 0xc5:
                switch (tandy.dac.mode&3) {
                case 0:
                    // joystick mode
                    break;
                case 1:
                    tandy.dac.control = (/*Bit8u*/short)(data&0xff);
                    break;
                case 2:
                    break;
                case 3:
                    // direct output
                    break;
                }
                break;
            case 0xc6:
                tandy.dac.frequency = tandy.dac.frequency & 0xf00 | (/*Bit8u*/short)(data&0xff);
                switch (tandy.dac.mode&3) {
                case 0:
                    // joystick mode
                    break;
                case 1:
                case 2:
                case 3:
                    TandyDACModeChanged();
                    break;
                }
                break;
            case 0xc7:
                tandy.dac.frequency = tandy.dac.frequency & 0x00ff | (((/*Bit8u*/short)(data&0xf))<<8);
                tandy.dac.amplitude = (/*Bit8u*/short)(data>>5);
                switch (tandy.dac.mode&3) {
                case 0:
                    // joystick mode
                    break;
                case 1:
                case 2:
                case 3:
                    TandyDACModeChanged();
                    break;
                }
                break;
            }
        }
    };

    static private final IoHandler.IO_ReadHandler TandyDACRead = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            switch (port) {
            case 0xc4:
                return (tandy.dac.mode&0x77) | (tandy.dac.irq_activated ? 0x08 : 0x00);
            case 0xc6:
                return (/*Bit8u*/short)(tandy.dac.frequency&0xff);
            case 0xc7:
                return (/*Bit8u*/short)(((tandy.dac.frequency>>8)&0xf) | (tandy.dac.amplitude<<5));
            }
            Log.log_msg("Tandy DAC: Read from unknown "+Integer.toString(port,16));
            return 0xff;
        }
    };

    private static void TandyDACGenerateDMASound(/*Bitu*/int length) {
        if (length!=0) {
            /*Bitu*/int read=tandy.dac.dma.chan.Read(length,tandy.dac.dma.buf, 0);
            tandy.dac.chan.AddSamples_m8(read,tandy.dac.dma.buf);
            if (read < length) {
                if (read>0) tandy.dac.dma.last_sample[0]=tandy.dac.dma.buf[read-1];
                for (/*Bitu*/int ct=read; ct < length; ct++) {
                    tandy.dac.chan.AddSamples_m8(1,tandy.dac.dma.last_sample);
                }
            }
        }
    }
    
    private static final Mixer.MIXER_Handler TandyDACUpdate = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int length) {
            if (tandy.dac.enabled && ((tandy.dac.mode&0x0c)==0x0c)) {
                if (!tandy.dac.dma.transfer_done) {
                    /*Bitu*/int len = length;
                    TandyDACGenerateDMASound(len);
                } else {
                    for (/*Bitu*/int ct=0; ct < length; ct++) {
                        tandy.dac.chan.AddSamples_m8(1,tandy.dac.dma.last_sample);
                    }
                }
            } else {
                tandy.dac.chan.AddSilence();
            }
        }
    };
    
    private IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[4];
    private IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[4];
    private Mixer.MixerObject MixerChan = new Mixer.MixerObject();
    private Mixer.MixerObject MixerChanDAC = new Mixer.MixerObject();

    public TandySound(Section configuration) {
        super(configuration);
        int i;
        Section_prop section=(Section_prop)(configuration);

        for (i=0;i<WriteHandler.length;i++) {
            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
        }
        for (i=0;i<ReadHandler.length;i++) {
            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        }
        boolean enable_hw_tandy_dac=true;
        /*Bitu*/IntRef sbport = new IntRef(0), sbirq = new IntRef(0), sbdma = new IntRef(0);
        if (SBlaster.SB_Get_Address(sbport, sbirq, sbdma)) {
            enable_hw_tandy_dac=false;
        }

        Memory.real_writeb(0x40, 0xd4, 0x00);
        if (Dosbox.IS_TANDY_ARCH()) {
            /* enable tandy sound if tandy=true/auto */
            String option = section.Get_string("tandy").toLowerCase();
            if (!option.equals("true") && !option.equals("on") && !option.equals("auto")) return;
        } else {
            /* only enable tandy sound if tandy=true */
            String option = section.Get_string("tandy").toLowerCase();
            if (!option.equals("true") && !option.equals("on")) return;

            /* ports from second DMA controller conflict with tandy ports */
            DMA.CloseSecondDMAController();

            if (enable_hw_tandy_dac) {
                WriteHandler[2].Install(0x1e0,SN76496Write,IoHandler.IO_MB,2);
                WriteHandler[3].Install(0x1e4,TandyDACWrite,IoHandler.IO_MB,4);
//				ReadHandler[3].Install(0x1e4,TandyDACRead,IO_MB,4);
            }
        }


        /*Bit32u*/int sample_rate = section.Get_int("tandyrate");
        tandy.chan=MixerChan.Install(SN76496Update,sample_rate,"TANDY");

        WriteHandler[0].Install(0xc0,SN76496Write,IoHandler.IO_MB,2);

        if (enable_hw_tandy_dac) {
            // enable low-level Tandy DAC emulation
            WriteHandler[1].Install(0xc4,TandyDACWrite,IoHandler.IO_MB,4);
            ReadHandler[1].Install(0xc4,TandyDACRead,IoHandler.IO_MB,4);

            tandy.dac.enabled=true;
            tandy.dac.chan=MixerChanDAC.Install(TandyDACUpdate,sample_rate,"TANDYDAC");

            tandy.dac.hw.base=0xc4;
            tandy.dac.hw.irq =7;
            tandy.dac.hw.dma =1;
        } else {
            tandy.dac.enabled=false;
            tandy.dac.hw.base=0;
            tandy.dac.hw.irq =0;
            tandy.dac.hw.dma =0;
        }

        tandy.dac.control=0;
        tandy.dac.mode =0;
        tandy.dac.irq_activated=false;
        tandy.dac.frequency=0;
        tandy.dac.amplitude=0;
        tandy.dac.dma.last_sample[0]=0;


        tandy.enabled=false;
        Memory.real_writeb(0x40,0xd4,0xff);	/* BIOS Tandy DAC initialization value */

        SN76496 R = sn;
        R.SampleRate = sample_rate;
        SN76496_set_clock(3579545);
        for (i = 0;i < 4;i++) R.Volume[i] = 0;
        R.LastRegister = 0;
        for (i = 0;i < 8;i+=2)
        {
            R.Register[i] = 0;
            R.Register[i + 1] = 0x0f;	/* volume = 0 */
        }

        for (i = 0;i < 4;i++)
        {
            R.Output[i] = 0;
            R.Period[i] = R.Count[i] = R.UpdateStep;
        }
        R.RNG = NG_PRESET;
        R.Output[3] = R.RNG & 1;
        SN76496_set_gain(0x1);
    }
    
    private static TandySound test;
    
    private static Section.SectionFunction TANDYSOUND_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
        }
    };

    public static Section.SectionFunction TANDYSOUND_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new TandySound(section);
            section.AddDestroyFunction(TANDYSOUND_ShutDown,true);
        }
    };
}
