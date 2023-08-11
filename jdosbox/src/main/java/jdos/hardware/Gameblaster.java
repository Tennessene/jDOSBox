package jdos.hardware;

import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.ShortPtr;

public class Gameblaster {
    static final private int LEFT = 0x00;
    static final private int RIGHT = 0x01;
    static final private int CMS_BUFFER_SIZE = 128;
    static final private int CMS_RATE = 22050;

    /* this structure defines a channel */
    private static class saa1099_channel
    {
        int frequency;			/* frequency (0x00..0xff) */
        int freq_enable;		/* frequency enable */
        int noise_enable;		/* noise enable */
        int octave; 			/* octave (0x00..0x07) */
        int[] amplitude=new int[2];		/* amplitude (0x00..0x0f) */
        int[] envelope=new int[2];		/* envelope (0x00..0x0f or 0x10 == off) */

        /* vars to simulate the square wave */
        double counter;
        double freq;
        int level;
    }

    /* this structure defines a noise channel */
    private static class saa1099_noise
    {
        /* vars to simulate the noise generator output */
        double counter;
        double freq;
        int level;						/* noise polynomal shifter */
    }

    /* this structure defines a SAA1099 chip */
    private static class SAA1099
    {
        public SAA1099() {
            for (int i=0;i<channels.length;i++) {
                channels[i] = new saa1099_channel();
            }
            for (int i=0;i<noise.length;i++) {
                noise[i] = new saa1099_noise();
            }
        }
        int stream;						/* our stream */
        int[] noise_params=new int[2];			/* noise generators parameters */
        int[] env_enable=new int[2];				/* envelope generators enable */
        int[] env_reverse_right=new int[2];		/* envelope reversed for right channel */
        int[] env_mode=new int[2];				/* envelope generators mode */
        int[] env_bits=new int[2];				/* non zero = 3 bits resolution */
        int[] env_clock=new int[2];				/* envelope clock mode (non-zero external) */
        int[] env_step=new int[2];                /* current envelope step */
        int all_ch_enable;				/* all channels enable */
        int sync_state;					/* sync all channels */
        int selected_reg;				/* selected register */
        saa1099_channel[] channels=new saa1099_channel[6];    /* channels */
        saa1099_noise[] noise=new saa1099_noise[2];	/* noise generators */
    }

    private final static /*UINT8*/byte[][] envelope = new byte[][] {
        /* zero amplitude */
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        /* maximum amplitude */
        {15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,
         15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,
         15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,
         15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15, },
        /* single decay */
        {15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        /* repetitive decay */
        {15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
         15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
         15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
         15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 },
        /* single triangular */
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
         15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        /* repetitive triangular */
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
         15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
          0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
         15,14,13,12,11,10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 },
        /* single attack */
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        /* repetitive attack */
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
          0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
          0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
          0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15 }
    };


    static final private int[] amplitude_lookup = new int[] {
         0*32767/16,  1*32767/16,  2*32767/16,	3*32767/16,
         4*32767/16,  5*32767/16,  6*32767/16,	7*32767/16,
         8*32767/16,  9*32767/16, 10*32767/16, 11*32767/16,
        12*32767/16, 13*32767/16, 14*32767/16, 15*32767/16
    };

    /* global parameters */
    private static double sample_rate;
    private static SAA1099[] saa1099 = new SAA1099[2];
    private static Mixer.MixerChannel cms_chan;
    private static /*Bit16s*/ShortPtr[][] cms_buffer=new ShortPtr[2][2];

    static {
        cms_buffer[0][0] = new ShortPtr(CMS_BUFFER_SIZE);
        cms_buffer[0][1] = new ShortPtr(CMS_BUFFER_SIZE);
        cms_buffer[1][0] = new ShortPtr(CMS_BUFFER_SIZE);
        cms_buffer[1][1] = new ShortPtr(CMS_BUFFER_SIZE);
        cms_buf_point0 = new ShortPtr[] {cms_buffer[0][0],cms_buffer[0][1]};
        cms_buf_point2 = new ShortPtr[] {cms_buffer[1][0],cms_buffer[1][1]};
    }
    private static /*Bit16s*/ ShortPtr[] cms_buf_point0;
    private static /*Bit16s*/ ShortPtr[] cms_buf_point2;

    private static /*Bitu*/int last_command;
    private static /*Bitu*/int base_port;


    static void saa1099_envelope(int chip, int ch)
    {
        SAA1099 saa = saa1099[chip];
        if (saa.env_enable[ch]!=0)
        {
            int step, mode, mask;
            mode = saa.env_mode[ch];
            /* step from 0..63 and then loop in steps 32..63 */
            step = saa.env_step[ch] =
                ((saa.env_step[ch] + 1) & 0x3f) | (saa.env_step[ch] & 0x20);

            mask = 15;
            if (saa.env_bits[ch]!=0)
                mask &= ~1; 	/* 3 bit resolution, mask LSB */

            saa.channels[ch*3+0].envelope[ LEFT] =
            saa.channels[ch*3+1].envelope[ LEFT] =
            saa.channels[ch*3+2].envelope[ LEFT] = envelope[mode][step] & mask;
            if ((saa.env_reverse_right[ch] & 0x01)!=0)
            {
                saa.channels[ch*3+0].envelope[RIGHT] =
                saa.channels[ch*3+1].envelope[RIGHT] =
                saa.channels[ch*3+2].envelope[RIGHT] = (15 - envelope[mode][step]) & mask;
            }
            else
            {
                saa.channels[ch*3+0].envelope[RIGHT] =
                saa.channels[ch*3+1].envelope[RIGHT] =
                saa.channels[ch*3+2].envelope[RIGHT] = envelope[mode][step] & mask;
            }
        }
        else
        {
            /* envelope mode off, set all envelope factors to 16 */
            saa.channels[ch*3+0].envelope[ LEFT] =
            saa.channels[ch*3+1].envelope[ LEFT] =
            saa.channels[ch*3+2].envelope[ LEFT] =
            saa.channels[ch*3+0].envelope[RIGHT] =
            saa.channels[ch*3+1].envelope[RIGHT] =
            saa.channels[ch*3+2].envelope[RIGHT] = 16;
        }
    }


    private static void saa1099_update(int chip, ShortPtr[] buffer, int length)
    {
        SAA1099 saa = saa1099[chip];
        int j, ch;

        /* if the channels are disabled we're done */
        if (saa.all_ch_enable==0)
        {
            /* init output data */
            buffer[LEFT].clear(length*2);
            buffer[RIGHT].clear(length*2);
            return;
        }

        for (ch = 0; ch < 2; ch++)
        {
            switch (saa.noise_params[ch])
            {
            case 0: saa.noise[ch].freq = 31250.0 * 2; break;
            case 1: saa.noise[ch].freq = 15625.0 * 2; break;
            case 2: saa.noise[ch].freq =  7812.5 * 2; break;
            case 3: saa.noise[ch].freq = saa.channels[ch * 3].freq; break;
            }
        }

        /* fill all data needed */
        for( j = 0; j < length; j++ )
        {
            int output_l = 0, output_r = 0;

            /* for each channel */
            for (ch = 0; ch < 6; ch++)
            {
                if (saa.channels[ch].freq == 0.0)
                    saa.channels[ch].freq = (double)((2 * 15625) << saa.channels[ch].octave) /
                        (511.0 - (double)saa.channels[ch].frequency);

                /* check the actual position in the square wave */
                saa.channels[ch].counter -= saa.channels[ch].freq;
                while (saa.channels[ch].counter < 0)
                {
                    /* calculate new frequency now after the half wave is updated */
                    saa.channels[ch].freq = (double)((2 * 15625) << saa.channels[ch].octave) /
                        (511.0 - (double)saa.channels[ch].frequency);

                    saa.channels[ch].counter += sample_rate;
                    saa.channels[ch].level ^= 1;

                    /* eventually clock the envelope counters */
                    if (ch == 1 && saa.env_clock[0] == 0)
                        saa1099_envelope(chip, 0);
                    if (ch == 4 && saa.env_clock[1] == 0)
                        saa1099_envelope(chip, 1);
                }

                /* if the noise is enabled */
                if (saa.channels[ch].noise_enable!=0)
                {
                    /* if the noise level is high (noise 0: chan 0-2, noise 1: chan 3-5) */
                    if ((saa.noise[ch/3].level & 1)!=0)
                    {
                        /* subtract to avoid overflows, also use only half amplitude */
                        output_l -= saa.channels[ch].amplitude[ LEFT] * saa.channels[ch].envelope[ LEFT] / 16 / 2;
                        output_r -= saa.channels[ch].amplitude[RIGHT] * saa.channels[ch].envelope[RIGHT] / 16 / 2;
                    }
                }

                /* if the square wave is enabled */
                if (saa.channels[ch].freq_enable!=0)
                {
                    /* if the channel level is high */
                    if ((saa.channels[ch].level & 1)!=0)
                    {
                        output_l += saa.channels[ch].amplitude[ LEFT] * saa.channels[ch].envelope[ LEFT] / 16;
                        output_r += saa.channels[ch].amplitude[RIGHT] * saa.channels[ch].envelope[RIGHT] / 16;
                    }
                }
            }

            for (ch = 0; ch < 2; ch++)
            {
                /* check the actual position in noise generator */
                saa.noise[ch].counter -= saa.noise[ch].freq;
                while (saa.noise[ch].counter < 0)
                {
                    saa.noise[ch].counter += sample_rate;
                    if( ((saa.noise[ch].level & 0x4000) == 0) == ((saa.noise[ch].level & 0x0040) == 0) )
                        saa.noise[ch].level = (saa.noise[ch].level << 1) | 1;
                    else
                        saa.noise[ch].level <<= 1;
                }
            }
            /* write sound data to the buffer */
            buffer[LEFT].set(j, output_l / 6);
            buffer[RIGHT].set(j, output_r / 6);
        }
    }

    private static void saa1099_write_port_w( int chip, int offset, int data )
    {
        SAA1099 saa = saa1099[chip];
        if(offset == 1) {
            // address port
            saa.selected_reg = data & 0x1f;
            if (saa.selected_reg == 0x18 || saa.selected_reg == 0x19) {
                /* clock the envelope channels */
                if (saa.env_clock[0]!=0) saa1099_envelope(chip,0);
                if (saa.env_clock[1]!=0) saa1099_envelope(chip,1);
            }
            return;
        }
        int reg = saa.selected_reg;
        int ch;

        switch (reg)
        {
        /* channel i amplitude */
        case 0x00:	case 0x01:	case 0x02:	case 0x03:	case 0x04:	case 0x05:
            ch = reg & 7;
            saa.channels[ch].amplitude[LEFT] = amplitude_lookup[data & 0x0f];
            saa.channels[ch].amplitude[RIGHT] = amplitude_lookup[(data >> 4) & 0x0f];
            break;
        /* channel i frequency */
        case 0x08:	case 0x09:	case 0x0a:	case 0x0b:	case 0x0c:	case 0x0d:
            ch = reg & 7;
            saa.channels[ch].frequency = data & 0xff;
            break;
        /* channel i octave */
        case 0x10:	case 0x11:	case 0x12:
            ch = (reg - 0x10) << 1;
            saa.channels[ch + 0].octave = data & 0x07;
            saa.channels[ch + 1].octave = (data >> 4) & 0x07;
            break;
        /* channel i frequency enable */
        case 0x14:
            saa.channels[0].freq_enable = data & 0x01;
            saa.channels[1].freq_enable = data & 0x02;
            saa.channels[2].freq_enable = data & 0x04;
            saa.channels[3].freq_enable = data & 0x08;
            saa.channels[4].freq_enable = data & 0x10;
            saa.channels[5].freq_enable = data & 0x20;
            break;
        /* channel i noise enable */
        case 0x15:
            saa.channels[0].noise_enable = data & 0x01;
            saa.channels[1].noise_enable = data & 0x02;
            saa.channels[2].noise_enable = data & 0x04;
            saa.channels[3].noise_enable = data & 0x08;
            saa.channels[4].noise_enable = data & 0x10;
            saa.channels[5].noise_enable = data & 0x20;
            break;
        /* noise generators parameters */
        case 0x16:
            saa.noise_params[0] = data & 0x03;
            saa.noise_params[1] = (data >> 4) & 0x03;
            break;
        /* envelope generators parameters */
        case 0x18:	case 0x19:
            ch = reg - 0x18;
            saa.env_reverse_right[ch] = data & 0x01;
            saa.env_mode[ch] = (data >> 1) & 0x07;
            saa.env_bits[ch] = data & 0x10;
            saa.env_clock[ch] = data & 0x20;
            saa.env_enable[ch] = data & 0x80;
            /* reset the envelope */
            saa.env_step[ch] = 0;
            break;
        /* channels enable & reset generators */
        case 0x1c:
            saa.all_ch_enable = data & 0x01;
            saa.sync_state = data & 0x02;
            if ((data & 0x02)!=0)
            {
                int i;
    //			logerror("%04x: (SAA1099 #%d) -reg 0x1c- Chip reset\n",activecpu_get_pc(), chip);
                /* Synch & Reset generators */
                for (i = 0; i < 6; i++)
                {
                    saa.channels[i].level = 0;
                    saa.channels[i].counter = 0.0;
                }
            }
            break;
        default:	/* Error! */
    //		logerror("%04x: (SAA1099 #%d) Unknown operation (reg:%02x, data:%02x)\n",activecpu_get_pc(), chip, reg, data);
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR,"CMS Unkown write to reg "+Integer.toString(reg,16)+" with "+Integer.toString(data,16));
        }
    }

    private static IoHandler.IO_WriteHandler write_cms = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if(cms_chan!=null && (!cms_chan.enabled)) cms_chan.Enable(true);
            last_command = Pic.PIC_Ticks;
            switch (port-base_port) {
            case 0:
                saa1099_write_port_w(0,0,val);
                break;
            case 1:
                saa1099_write_port_w(0,1,val);
                break;
            case 2:
                saa1099_write_port_w(1,0,val);
                break;
            case 3:
                saa1099_write_port_w(1,1,val);
                break;
            }
        }
    };

    private static Mixer.MIXER_Handler CMS_CallBack = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int len) {
            if (len > CMS_BUFFER_SIZE) return;

            saa1099_update(0, cms_buf_point0, (int)len);
            saa1099_update(1, cms_buf_point2, (int)len);

             /*Bit16s*/short[] stream=Mixer.MixTemp16;
            int streamOff = 0;
            /* Mix chip outputs */
            for (/*Bitu*/int l=0;l<len;l++) {
                /*Bits*/short left, right;
                left = (short)(cms_buffer[0][LEFT].get(l) + cms_buffer[1][LEFT].get(l));
                right = (short)(cms_buffer[0][RIGHT].get(l) + cms_buffer[1][RIGHT].get(l));

                if (left>Mixer.MAX_AUDIO) stream[streamOff++]=Mixer.MAX_AUDIO;
                else if (left<Mixer.MIN_AUDIO) stream[streamOff++]=Mixer.MIN_AUDIO;
                else stream[streamOff++]=left;

                if (right>Mixer.MAX_AUDIO) stream[streamOff++]=Mixer.MAX_AUDIO;
                else if (right<Mixer.MIN_AUDIO) stream[streamOff++]=Mixer.MIN_AUDIO;
                else stream[streamOff++]=right;

            }
            if(cms_chan!=null) cms_chan.AddSamples_s16(len,stream);
            if (last_command + 10000 < Pic.PIC_Ticks) if(cms_chan!=null) cms_chan.Enable(false);
        }
    };

    // The Gameblaster detection
    private static /*Bit8u*/short cms_detect_register = 0xff;

    static private IoHandler.IO_WriteHandler write_cms_detect = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch(port-base_port) {
            case 0x6:
            case 0x7:
                cms_detect_register = (short)val;
                break;
            }
        }
    };

    static private IoHandler.IO_ReadHandler read_cms_detect = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bit8u*/short retval = 0xff;
            switch(port-base_port) {
            case 0x4:
                retval = 0x7f;
                break;
            case 0xa:
            case 0xb:
                retval = cms_detect_register;
                break;
            }
            return retval;
        }
    };

    static private class CMS extends Module_base {
        private IoHandler.IO_WriteHandleObject WriteHandler = new IoHandler.IO_WriteHandleObject();
        private IoHandler.IO_WriteHandleObject DetWriteHandler = new IoHandler.IO_WriteHandleObject();
        private IoHandler.IO_ReadHandleObject DetReadHandler = new IoHandler.IO_ReadHandleObject();
        private Mixer.MixerObject MixerChan = new Mixer.MixerObject();

        public CMS(Section configuration) {
            super(configuration);
            Section_prop section = (Section_prop)(configuration);
            /*Bitu*/int sample_rate_temp = section.Get_int("oplrate");
            sample_rate = (double)(sample_rate_temp);
            base_port = section.Get_hex("sbbase").toInt();
            WriteHandler.Install(base_port, write_cms, IoHandler.IO_MB,4);

            // A standalone Gameblaster has a magic chip on it which is
            // sometimes used for detection.
            String sbtype=section.Get_string("sbtype");
            if (sbtype.equalsIgnoreCase("gb")) {
                DetWriteHandler.Install(base_port+4,write_cms_detect,IoHandler.IO_MB,12);
                DetReadHandler.Install(base_port,read_cms_detect,IoHandler.IO_MB,16);
            }

            /* Register the Mixer CallBack */
            cms_chan = MixerChan.Install(CMS_CallBack,sample_rate_temp,"CMS");

            last_command = Pic.PIC_Ticks;

        }
        public void close() {
            cms_chan = null;
        }
    }

    private static CMS test;

    static public void CMS_Init(Section sec) {
        for (int i=0;i<saa1099.length;i++)
            saa1099[i] = new SAA1099();
        test = new CMS(sec);
    }

    static public void CMS_ShutDown(Section sec) {
        test.close();
        test = null;
        for (int i=0;i<saa1099.length;i++)
            saa1099[i] = null;
    }

}
