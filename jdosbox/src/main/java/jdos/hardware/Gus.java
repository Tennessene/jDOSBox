/*
 *  Copyright (C) 2002-2012  The DOSBox Team
 *  Copyright (C) 2012 Kevin O'Dwyer (jsDOSBox - JavaScript port of jDosbox)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package jdos.hardware;

import jdos.Dosbox;
import jdos.hardware.DMA.DMAEvent;
import jdos.hardware.IoHandler.IO_ReadHandleObject;
import jdos.hardware.IoHandler.IO_WriteHandleObject;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.shell.AutoexecObject;

public class Gus extends Module_base {
    private static GFGus myGUS = new GFGus();

    //Extra bits of precision over normal gus
    private static final int WAVE_BITS = 2;
    private static final int WAVE_FRACT = (9 + WAVE_BITS);
    private static final int WAVE_FRACT_MASK = ((1 << WAVE_FRACT) - 1);
    private static final long WAVE_MSWMASK = ((1 << (16 + WAVE_BITS)) - 1);
    private static final long WAVE_LSWMASK = (0xffffffffl ^ WAVE_MSWMASK);

    //Amount of precision the volume has
    private static final int RAMP_FRACT = 10;

    //private static final int LOG_GUS =0;

    private static Mixer.MixerChannel gus_chan;
    private static byte irqtable[] = new byte[]{0, 2, 5, 3, 7, 11, 12, 15};
    private static byte dmatable[] = new byte[]{0, 1, 3, 5, 6, 7, 0, 0};
    private static byte GUSRam[] = null; // 1024K of GUS Ram
    private static int AutoAmp = 512;
    private static char[] vol16bit = new char[4096];
    private static long[] pantable = new long[16];

    public static /*short*/ short adlib_commandreg;
    private static GUSChannels[] guschan = new GUSChannels[32];
    private static GUSChannels curchan;

    private IO_ReadHandleObject[] ReadHandler = new IO_ReadHandleObject[8];
    private IO_WriteHandleObject[] WriteHandler = new IO_WriteHandleObject[9];
    private AutoexecObject[] autoexecline = new AutoexecObject[2];
    private Mixer.MixerObject MixerChan = new Mixer.MixerObject();

    private class GUSChannels {
        private long WaveStart;
        private long WaveEnd;
        private long WaveAddr;
        private long WaveAdd;
        private short WaveCtrl;
        private char WaveFreq;

        private long RampStart;
        private long RampEnd;
        private long RampVol;
        private long RampAdd;

        private short RampRate;
        private short RampCtrl;

        private short PanPot;
        private long irqmask;
        private long PanLeft;
        private long PanRight;
        private int VolLeft;
        private int VolRight;

        private GUSChannels(short num) {
            irqmask = 1 << num;
            WaveStart = 0;
            WaveEnd = 0;
            WaveAddr = 0;
            WaveAdd = 0;
            WaveFreq = 0;
            WaveCtrl = 3;
            RampRate = 0;
            RampStart = 0;
            RampEnd = 0;
            RampCtrl = 3;
            RampAdd = 0;
            RampVol = 0;
            VolLeft = 0;
            VolRight = 0;
            PanLeft = 0;
            PanRight = 0;
            PanPot = 0x7;
        }

        private void WriteWaveFreq(char val) {
            WaveFreq = val;
            double frameadd = (val >> 1) / 512.0d;        //Samples / original gus frame
            double realadd = (frameadd * (double) myGUS.basefreq / (double) myGUS.rate) * (double) (1 << WAVE_FRACT);
            WaveAdd = (long) realadd;
        }

        private void WriteWaveCtrl(short val) {
            long oldirq = myGUS.WaveIRQ;
            WaveCtrl = (short) (val & 0x7f);
            if ((val & 0xa0) == 0xa0) myGUS.WaveIRQ |= irqmask;
            else myGUS.WaveIRQ &= ~irqmask;
            if (oldirq != myGUS.WaveIRQ) {
                CheckVoiceIrq();
            }
        }

        private short ReadWaveCtrl() {
            short ret = WaveCtrl;
            if ((myGUS.WaveIRQ & irqmask) != 0) {
                ret |= 0x80;
            }
            return ret;
        }

        private void UpdateWaveRamp() {
            WriteWaveFreq(WaveFreq);
            WriteRampRate(RampRate);
        }

        private void WritePanPot(short val) {
            PanPot = val;
            PanLeft = pantable[0x0f - (val & 0xf)];
            PanRight = pantable[(val & 0xf)];
            UpdateVolumes();
        }

        private void WriteRampCtrl(short val) {
            long old = myGUS.RampIRQ;
            RampCtrl = (short) (val & 0x7f);
            if ((val & 0xa0) == 0xa0) myGUS.RampIRQ |= irqmask;
            else myGUS.RampIRQ &= ~irqmask;
            if (old != myGUS.RampIRQ) CheckVoiceIrq();
        }

        private short ReadRampCtrl() {
            short ret = RampCtrl;
            if ((myGUS.RampIRQ & irqmask) != 0) ret |= 0x80;
            return ret;
        }

        private void WriteRampRate(short val) {
            RampRate = val;
            double frameadd = (double) (RampRate & 63) / (double) (1 << (3 * (val >> 6)));
            double realadd = (frameadd * (double) myGUS.basefreq / (double) myGUS.rate) * (double) (1 << RAMP_FRACT);
            RampAdd = (long) realadd;
        }

        private void WaveUpdate() {
            if ((WaveCtrl & 0x3) != 0) return;
            int WaveLeft;
            if ((WaveCtrl & 0x40) != 0) {
                WaveAddr -= WaveAdd;
                WaveLeft = (int) (WaveStart - WaveAddr);
            } else {
                WaveAddr += WaveAdd;
                WaveLeft = (int) (WaveAddr - WaveEnd);
            }
            if (WaveLeft < 0) return;
            /* Generate an IRQ if needed */
            if ((WaveCtrl & 0x20) != 0) {
                myGUS.WaveIRQ |= irqmask;
            }
            /* Check for not being in PCM operation */
            if ((RampCtrl & 0x04) != 0) return;
            /* Check for looping */
            if ((WaveCtrl & 0x08) != 0) {
                /* Bi-directional looping */
                if ((WaveCtrl & 0x10) != 0) WaveCtrl ^= 0x40;
                WaveAddr = ((WaveCtrl & 0x40) != 0) ? (WaveEnd - WaveLeft) : (WaveStart + WaveLeft);
            } else {
                WaveCtrl |= 1;    //Stop the channel
                WaveAddr = ((WaveCtrl & 0x40) != 0) ? WaveStart : WaveEnd;
            }
        }

        private void UpdateVolumes() {
            int templeft = (int) (RampVol - PanLeft);
            templeft &= ~(templeft >> 31);
            int tempright = (int) (RampVol - PanRight);
            tempright &= ~(tempright >> 31);
            VolLeft = vol16bit[templeft >> RAMP_FRACT];
            VolRight = vol16bit[tempright >> RAMP_FRACT];
        }

        private void RampUpdate() {
            /* Check if ramping enabled */
            if ((RampCtrl & 0x3) != 0) return;
            int RampLeft;
            if ((RampCtrl & 0x40) != 0) {
                RampVol -= RampAdd;
                RampLeft = (int) (RampStart - RampVol);
            } else {
                RampVol += RampAdd;
                RampLeft = (int) (RampVol - RampEnd);
            }
            if (RampLeft < 0) {
                UpdateVolumes();
                return;
            }
            /* Generate an IRQ if needed */
            if ((RampCtrl & 0x20) != 0) {
                myGUS.RampIRQ |= irqmask;
            }
            /* Check for looping */
            if ((RampCtrl & 0x08) != 0) {
                /* Bi-directional looping */
                if ((RampCtrl & 0x10) != 0) RampCtrl ^= 0x40;
                RampVol = ((RampCtrl & 0x40) != 0) ? (RampEnd - RampLeft) : (RampStart + RampLeft);
            } else {
                RampCtrl |= 1;    //Stop the channel
                RampVol = ((RampCtrl & 0x40) != 0) ? RampStart : RampEnd;
            }
            UpdateVolumes();
        }

        private void generateSamples(int[] stream, long len) {
            int i;
            int tmpsamp;
            boolean eightbit;
            if ((RampCtrl & WaveCtrl & 3) != 0) return;
            eightbit = ((WaveCtrl & 0x4) == 0);

            for (i = 0; i < (int) len; i++) {
                // Get sample
                tmpsamp = GetSample(WaveAdd, WaveAddr, eightbit);
                // Output stereo sample
                stream[i << 1] += tmpsamp * VolLeft;
                stream[(i << 1) + 1] += tmpsamp * VolRight;
                WaveUpdate();
                RampUpdate();
            }
        }
    }

    // Returns a single 16-bit sample from the Gravis's RAM
    private static int GetSample(long Delta, long CurAddr, boolean eightbit) {
        int useAddr = (int) (CurAddr >> WAVE_FRACT);
        int holdAddr;

        if (eightbit) {
            if (Delta >= (1 << WAVE_FRACT)) {
                int tmpsmall = GUSRam[useAddr];
                return tmpsmall << 8;
            } else {
                // Interpolate
                int w1 = GUSRam[useAddr + 0] << 8; // intentional signed shift - Bit32s w1 = ((Bit8s)GUSRam[useAddr+0]) << 8;
                int w2 = GUSRam[useAddr + 1] << 8; // intentional signed shift - Bit32s w2 = ((Bit8s)GUSRam[useAddr+1]) << 8;
                int diff = w2 - w1;
                return (int) (w1 + ((diff * (CurAddr & WAVE_FRACT_MASK)) >> WAVE_FRACT));
            }
        } else {
            // Formula used to convert addresses for use with 16-bit samples
            holdAddr = useAddr & 0xc0000;
            useAddr = useAddr & 0x1ffff;
            useAddr = useAddr << 1;
            useAddr = (holdAddr | useAddr);

            if (Delta >= (1 << WAVE_FRACT)) {
                return (GUSRam[useAddr + 0] & 0xFF) | ((GUSRam[useAddr + 1] & 0xFF) << 8);
            } else {
                // Interpolate
                int w1 = (GUSRam[useAddr + 0] & 0xFF) | (GUSRam[useAddr + 1] << 8); // intentional signed shift - Bit32s w1 = (GUSRam[useAddr+0] | (((Bit8s)GUSRam[useAddr+1]) << 8));
                int w2 = (GUSRam[useAddr + 2] & 0xFF) | (GUSRam[useAddr + 3] << 8); // intentional signed shift - Bit32s w2 = (GUSRam[useAddr+2] | (((Bit8s)GUSRam[useAddr+3]) << 8));
                int diff = w2 - w1;
                return (int) (w1 + ((diff * (CurAddr & WAVE_FRACT_MASK)) >> WAVE_FRACT));
            }
        }
    }

    public Gus(Section configuration) {
        super(configuration);

        if (!Dosbox.IS_EGAVGA_ARCH()) return;
        Section_prop section = (Section_prop) configuration;
        if (!section.Get_bool("gus")) return;
        GUSRam = new byte[1024 * 1024]; // only allocate RAM if the device is enabled

        myGUS.rate = section.Get_int("gusrate");

        myGUS.portbase = section.Get_hex("gusbase").toInt() - 0x200;
        int dma_val = section.Get_int("gusdma");
        if ((dma_val < 0) || (dma_val > 255)) dma_val = 3;    // sensible default
        int irq_val = section.Get_int("gusirq");
        if ((irq_val < 0) || (irq_val > 255)) irq_val = 5;    // sensible default
        myGUS.dma1 = (short) dma_val;
        myGUS.dma2 = (short) dma_val;
        myGUS.irq1 = (short) irq_val;
        myGUS.irq2 = (short) irq_val;

        // We'll leave the MIDI interface to the MPU-401
        // Ditto for the Joystick
        // GF1 Synthesizer
        //            WriteHandler[0].Install(0x388,adlib_gusforward,IoHandler.IoHandler.IO_MB);

        for (int i = 0; i < ReadHandler.length; i++)
            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        for (int i = 0; i < WriteHandler.length; i++)
            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();

        ReadHandler[0].Install(0x302 + myGUS.portbase, read_gus, IoHandler.IO_MB);
        WriteHandler[0].Install(0x302 + myGUS.portbase, write_gus, IoHandler.IO_MB);

        WriteHandler[1].Install(0x303 + myGUS.portbase, write_gus, IoHandler.IO_MB);
        ReadHandler[1].Install(0x303 + myGUS.portbase, read_gus, IoHandler.IO_MB);

        WriteHandler[2].Install(0x304 + myGUS.portbase, write_gus, IoHandler.IO_MB | IoHandler.IO_MW);
        ReadHandler[2].Install(0x304 + myGUS.portbase, read_gus, IoHandler.IO_MB | IoHandler.IO_MW);

        WriteHandler[3].Install(0x305 + myGUS.portbase, write_gus, IoHandler.IO_MB);
        ReadHandler[3].Install(0x305 + myGUS.portbase, read_gus, IoHandler.IO_MB);

        ReadHandler[4].Install(0x206 + myGUS.portbase, read_gus, IoHandler.IO_MB);

        WriteHandler[4].Install(0x208 + myGUS.portbase, write_gus, IoHandler.IO_MB);
        ReadHandler[5].Install(0x208 + myGUS.portbase, read_gus, IoHandler.IO_MB);

        WriteHandler[5].Install(0x209 + myGUS.portbase, write_gus, IoHandler.IO_MB);

        WriteHandler[6].Install(0x307 + myGUS.portbase, write_gus, IoHandler.IO_MB);
        ReadHandler[6].Install(0x307 + myGUS.portbase, read_gus, IoHandler.IO_MB);

        // Board Only

        WriteHandler[7].Install(0x200 + myGUS.portbase, write_gus, IoHandler.IO_MB);
        ReadHandler[7].Install(0x20A + myGUS.portbase, read_gus, IoHandler.IO_MB);
        WriteHandler[8].Install(0x20B + myGUS.portbase, write_gus, IoHandler.IO_MB);

        //	DmaChannels[myGUS.dma1].Register_TC_Callback(GUS_DMA_TC_Callback);

        MakeTables();

        for (short chan_ct = 0; chan_ct < 32; chan_ct++) {
            guschan[chan_ct] = new GUSChannels(chan_ct);
        }
        // Register the Mixer CallBack
        gus_chan = MixerChan.Install(GUS_CallBack, myGUS.rate, "GUS");
        myGUS.gRegData = 0x1;
        GUSReset();
        myGUS.gRegData = 0x0;
        String portat = Integer.toHexString(0x200 + myGUS.portbase);

        // ULTRASND=Port,DMA1,DMA2,IRQ1,IRQ2
        // [GUS port], [GUS DMA (recording)], [GUS DMA (playback)], [GUS IRQ (playback)], [GUS IRQ (MIDI)]
        String temp = "SET ULTRASND=" + portat + ","
                + myGUS.dma1 + "," + myGUS.dma2 + ","
                + myGUS.irq1 + "," + myGUS.irq2;
        // Create autoexec.bat lines
        autoexecline[0] = new AutoexecObject();
        autoexecline[0].Install(temp);
        autoexecline[1] = new AutoexecObject();
        autoexecline[1].Install("SET ULTRADIR=" + section.Get_string("ultradir"));

    }

    private static void GUSReset() {
        if ((myGUS.gRegData & 0x1) == 0x1) {
            // Reset
            adlib_commandreg = 85;
            myGUS.IRQStatus = 0;
            myGUS.timers[0].raiseirq = false;
            myGUS.timers[1].raiseirq = false;
            myGUS.timers[0].reached = false;
            myGUS.timers[1].reached = false;
            myGUS.timers[0].running = false;
            myGUS.timers[1].running = false;

            myGUS.timers[0].value = 0xff;
            myGUS.timers[1].value = 0xff;
            myGUS.timers[0].delay = 0.080f;
            myGUS.timers[1].delay = 0.320f;

            myGUS.ChangeIRQDMA = false;
            myGUS.mixControl = 0x0b;    // latches enabled by default LINEs disabled
            // Stop all channels
            int i;
            for (i = 0; i < 32; i++) {
                guschan[i].RampVol = 0;
                guschan[i].WriteWaveCtrl((short) 0x1);
                guschan[i].WriteRampCtrl((short) 0x1);
                guschan[i].WritePanPot((short) 0x7);
            }
            myGUS.IRQChan = 0;
        }
        if ((myGUS.gRegData & 0x4) != 0) {
            myGUS.irqenabled = true;
        } else {
            myGUS.irqenabled = false;
        }
    }

    private static void GUS_CheckIRQ() {
        if (myGUS.IRQStatus != 0 && (myGUS.mixControl & 0x08) != 0) {
            Pic.PIC_ActivateIRQ(myGUS.irq1);
        }
    }

    private static void CheckVoiceIrq() {
        myGUS.IRQStatus &= 0x9f;
        long totalmask = (myGUS.RampIRQ | myGUS.WaveIRQ) & myGUS.ActiveMask;
        if (totalmask == 0) return;
        if (myGUS.RampIRQ != 0) myGUS.IRQStatus |= 0x40;
        if (myGUS.WaveIRQ != 0) myGUS.IRQStatus |= 0x20;
        GUS_CheckIRQ();
        for (; ; ) {
            long check = (1 << myGUS.IRQChan);
            if ((totalmask & check) != 0) return;
            myGUS.IRQChan++;
            if (myGUS.IRQChan >= myGUS.ActiveChannels) myGUS.IRQChan = 0;
        }
    }

    private static char ExecuteReadRegister() {
        short tmpreg;
//    	LOG_MSG("Read global reg %x",myGUS.gRegSelect);
        switch (myGUS.gRegSelect) {
            case 0x41: // Dma control register - read acknowledges DMA IRQ
                tmpreg = (short) (myGUS.DMAControl & 0xbf);
                tmpreg |= (myGUS.IRQStatus & 0x80) >> 1;
                myGUS.IRQStatus &= 0x7f;
                return (char) (tmpreg << 8);
            case 0x42:  // Dma address register
                return myGUS.dmaAddr;
            case 0x45:  // Timer control register.  Identical in operation to Adlib's timer
                return (char) (myGUS.TimerControl << 8);
            case 0x49:  // Dma sample register
                tmpreg = (short) (myGUS.DMAControl & 0xbf);
                tmpreg |= (myGUS.IRQStatus & 0x80) >> 1;
                return (char) (tmpreg << 8);
            case 0x80: // Channel voice control read register
                if (curchan != null) return (char) (curchan.ReadWaveCtrl() << 8);
                else return 0x0300;

            case 0x82: // Channel MSB start address register
                if (curchan != null) return (char) (curchan.WaveStart >> (WAVE_BITS + 16));
                else return 0x0000;
            case 0x83: // Channel LSW start address register
                if (curchan != null) return (char) (curchan.WaveStart >> WAVE_BITS);
                else return 0x0000;

            case 0x89: // Channel volume register
                if (curchan != null) return (char) ((curchan.RampVol >> RAMP_FRACT) << 4);
                else return 0x0000;
            case 0x8a: // Channel MSB current address register
                if (curchan != null) return (char) (curchan.WaveAddr >> (WAVE_BITS + 16));
                else return 0x0000;
            case 0x8b: // Channel LSW current address register
                if (curchan != null) return (char) (curchan.WaveAddr >> WAVE_BITS);
                else return 0x0000;

            case 0x8d: // Channel volume control register
                if (curchan != null) return (char) (curchan.ReadRampCtrl() << 8);
                else return 0x0300;
            case 0x8f: // General channel IRQ status register
                tmpreg = (short) (myGUS.IRQChan | 0x20);
                long mask;
                mask = 1 << myGUS.IRQChan;
                if ((myGUS.RampIRQ & mask) == 0) tmpreg |= 0x40;
                if ((myGUS.WaveIRQ & mask) == 0) tmpreg |= 0x80;
                myGUS.RampIRQ &= ~mask;
                myGUS.WaveIRQ &= ~mask;
                CheckVoiceIrq();
                return (char) (tmpreg << 8);
            default:
                //Log.log_msg("Read Register num 0x" + myGUS.gRegSelect);
                return myGUS.gRegData;
        }
    }

    private static Pic.PIC_EventHandler GUS_TimerEvent = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            if (!myGUS.timers[val].masked) myGUS.timers[val].reached = true;
            if (myGUS.timers[val].raiseirq) {
                myGUS.IRQStatus |= 0x4 << val;
                GUS_CheckIRQ();
            }
            if (myGUS.timers[val].running)
                Pic.PIC_AddEvent(GUS_TimerEvent, myGUS.timers[val].delay, val);
        }
    };

    private static void ExecuteGlobRegister() {
//    	if (myGUS.gRegSelect|1!=0x44) LOG_MSG("write global register %x with %x", myGUS.gRegSelect, myGUS.gRegData);
        switch (myGUS.gRegSelect) {
            case 0x0:  // Channel voice control register
                if (curchan != null) curchan.WriteWaveCtrl((short) (myGUS.gRegData >> 8));
                break;
            case 0x1:  // Channel frequency control register
                if (curchan != null) curchan.WriteWaveFreq(myGUS.gRegData);
                break;
            case 0x2:  // Channel MSW start address register
                if (curchan != null) {
                    long tmpaddr = (long) (myGUS.gRegData & 0x1fff) << (16 + WAVE_BITS);
                    curchan.WaveStart = (curchan.WaveStart & WAVE_MSWMASK) | tmpaddr;
                }
                break;
            case 0x3:  // Channel LSW start address register
                if (curchan != null) {
                    long tmpaddr = (long) (myGUS.gRegData) << WAVE_BITS;
                    curchan.WaveStart = (curchan.WaveStart & WAVE_LSWMASK) | tmpaddr;
                }
                break;
            case 0x4:  // Channel MSW end address register
                if (curchan != null) {
                    long tmpaddr = (long) (myGUS.gRegData & 0x1fff) << (16 + WAVE_BITS);
                    curchan.WaveEnd = (curchan.WaveEnd & WAVE_MSWMASK) | tmpaddr;
                }
                break;
            case 0x5:  // Channel MSW end address register
                if (curchan != null) {
                    long tmpaddr = (long) (myGUS.gRegData) << WAVE_BITS;
                    curchan.WaveEnd = (curchan.WaveEnd & WAVE_LSWMASK) | tmpaddr;
                }
                break;
            case 0x6:  // Channel volume ramp rate register
                if (curchan != null) {
                    short tmpdata = (short) ((myGUS.gRegData >> 8) & 0xff);
                    curchan.WriteRampRate(tmpdata);
                }
                break;
            case 0x7:  // Channel volume ramp start register  EEEEMMMM
                if (curchan != null) {
                    short tmpdata = (short) ((myGUS.gRegData >> 8) & 0xff);
                    curchan.RampStart = tmpdata << (4 + RAMP_FRACT);
                }
                break;
            case 0x8:  // Channel volume ramp end register  EEEEMMMM
                if (curchan != null) {
                    short tmpdata = (short) ((myGUS.gRegData >> 8) & 0xff);
                    curchan.RampEnd = tmpdata << (4 + RAMP_FRACT);
                }
                break;
            case 0x9:  // Channel current volume register
                if (curchan != null) {
                    char tmpdata = (char) (myGUS.gRegData >> 4);
                    curchan.RampVol = tmpdata << RAMP_FRACT;
                    curchan.UpdateVolumes();
                }
                break;
            case 0xA:  // Channel MSW current address register
                if (curchan != null) {
                    long tmpaddr = (myGUS.gRegData & 0x1fff) << (16 + WAVE_BITS);
                    curchan.WaveAddr = ((curchan.WaveAddr & WAVE_MSWMASK) | tmpaddr);
                }
                break;
            case 0xB:  // Channel LSW current address register
                if (curchan != null) {
                    long tmpaddr = (myGUS.gRegData) << (WAVE_BITS);
                    curchan.WaveAddr = ((curchan.WaveAddr & WAVE_LSWMASK) | tmpaddr);
                }
                break;
            case 0xC:  // Channel pan pot register
                if (curchan != null) curchan.WritePanPot((short) (myGUS.gRegData >> 8));
                break;
            case 0xD:  // Channel volume control register
                if (curchan != null) curchan.WriteRampCtrl((short) (myGUS.gRegData >> 8));
                break;
            case 0xE:  // Set active channel register
                myGUS.gRegSelect = (short) (myGUS.gRegData >> 8);        //JAZZ Jackrabbit seems to assume this?
                myGUS.ActiveChannels = (short) (1 + (((short) (myGUS.gRegData >> 8)) & 63));
                if (myGUS.ActiveChannels < 14) myGUS.ActiveChannels = 14;
                if (myGUS.ActiveChannels > 32) myGUS.ActiveChannels = 32;
                myGUS.ActiveMask = (0xffffffffl >> (32 - myGUS.ActiveChannels));
                gus_chan.Enable(true);
                myGUS.basefreq = (long) ((double) 1000000.0 / (1.619695497d * (double) (myGUS.ActiveChannels)));
                //Log.log_msg("GUS set to " + myGUS.ActiveChannels + " channels");
                for (int i = 0; i < myGUS.ActiveChannels; i++) guschan[i].UpdateWaveRamp();
                break;
            case 0x10:  // Undocumented register used in Fast Tracker 2
                break;
            case 0x41:  // Dma control register
                myGUS.DMAControl = (short) (myGUS.gRegData >> 8);
                DMA.GetDMAChannel(myGUS.dma1).Register_Callback(
                        ((myGUS.DMAControl & 0x1) != 0) ? GUS_DMA_Callback : null);
                break;
            case 0x42:  // Gravis DRAM DMA address register
                myGUS.dmaAddr = myGUS.gRegData;
                break;
            case 0x43:  // MSB Peek/poke DRAM position
                myGUS.gDramAddr = (0xff0000 & myGUS.gDramAddr) | (myGUS.gRegData);
                break;
            case 0x44:  // LSW Peek/poke DRAM position
                myGUS.gDramAddr = (0xffff & myGUS.gDramAddr) | ((myGUS.gRegData >> 8) << 16);
                break;
            case 0x45:  // Timer control register.  Identical in operation to Adlib's timer
                myGUS.TimerControl = (short) (myGUS.gRegData >> 8);
                myGUS.timers[0].raiseirq = (myGUS.TimerControl & 0x04) > 0;
                if (!myGUS.timers[0].raiseirq) myGUS.IRQStatus &= ~0x04;
                myGUS.timers[1].raiseirq = (myGUS.TimerControl & 0x08) > 0;
                if (!myGUS.timers[1].raiseirq) myGUS.IRQStatus &= ~0x08;
                break;
            case 0x46:  // Timer 1 control
                myGUS.timers[0].value = (short) (myGUS.gRegData >> 8);
                myGUS.timers[0].delay = (0x100 - myGUS.timers[0].value) * 0.080f;
                break;
            case 0x47:  // Timer 2 control
                myGUS.timers[1].value = (short) (myGUS.gRegData >> 8);
                myGUS.timers[1].delay = (0x100 - myGUS.timers[1].value) * 0.320f;
                break;
            case 0x49:  // DMA sampling control register
                myGUS.SampControl = (short) (myGUS.gRegData >> 8);
                DMA.GetDMAChannel(myGUS.dma1).Register_Callback(
                        ((myGUS.SampControl & 0x1) != 0) ? GUS_DMA_Callback : null);
                break;
            case 0x4c:  // GUS reset register
                GUSReset();
                break;
            default:
                //Log.log_msg("Unimplemented global register " + myGUS.gRegSelect + " -- " + myGUS.gRegData);
                break;
        }
        return;
    }

    private static IoHandler.IO_ReadHandler read_gus = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            //    	LOG_MSG("read from gus port %x",port);
            switch (port - myGUS.portbase) {
                case 0x206:
                    return myGUS.IRQStatus;
                case 0x208:
                    short tmptime;
                    tmptime = 0;
                    if (myGUS.timers[0].reached) tmptime |= (1 << 6);
                    if (myGUS.timers[1].reached) tmptime |= (1 << 5);
                    if ((tmptime & 0x60) != 0) tmptime |= (1 << 7);
                    if ((myGUS.IRQStatus & 0x04) != 0) tmptime |= (1 << 2);
                    if ((myGUS.IRQStatus & 0x08) != 0) tmptime |= (1 << 1);
                    return tmptime;
                case 0x20a:
                    return adlib_commandreg;
                case 0x302:
                    return (short) myGUS.gCurChannel;
                case 0x303:
                    return myGUS.gRegSelect;
                case 0x304:
                    if (iolen == 2) return ExecuteReadRegister() & 0xffff;
                    else return ExecuteReadRegister() & 0xff;
                case 0x305:
                    return ExecuteReadRegister() >> 8;
                case 0x307:
                    if (myGUS.gDramAddr < GUSRam.length) {
                        return GUSRam[(int) myGUS.gDramAddr] & 0xFF;
                    } else {
                        return 0;
                    }
                default:
                    //Log.log_msg("Read GUS at port 0x" + port);
                    break;
            }

            return 0xff;
        }
    };

    private static IoHandler.IO_WriteHandler write_gus = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val16, /*Bitu*/int iolen) {
            //short val=(short)(val16&0xffff);
            char val = (char) (val16);
            //    	LOG_MSG("Write gus port %x val %x",port,val);
            switch (port - myGUS.portbase) {
                case 0x200:
                    myGUS.mixControl = (short) val;
                    myGUS.ChangeIRQDMA = true;
                    return;
                case 0x208:
                    adlib_commandreg = (short) val;
                    break;
                case 0x209:
                    //TODO adlib_commandreg should be 4 for this to work else it should just latch the value
                    if ((val & 0x80) != 0) {
                        myGUS.timers[0].reached = false;
                        myGUS.timers[1].reached = false;
                        return;
                    }
                    myGUS.timers[0].masked = (val & 0x40) > 0;
                    myGUS.timers[1].masked = (val & 0x20) > 0;
                    if ((val & 0x1) != 0) {
                        if (!myGUS.timers[0].running) {
                            Pic.PIC_AddEvent(GUS_TimerEvent, myGUS.timers[0].delay, 0);
                            myGUS.timers[0].running = true;
                        }
                    } else myGUS.timers[0].running = false;
                    if ((val & 0x2) != 0) {
                        if (!myGUS.timers[1].running) {
                            Pic.PIC_AddEvent(GUS_TimerEvent, myGUS.timers[1].delay, 1);
                            myGUS.timers[1].running = true;
                        }
                    } else myGUS.timers[1].running = false;
                    break;
                //TODO Check if 0x20a register is also available on the gus like on the interwave
                case 0x20b:
                    if (!myGUS.ChangeIRQDMA) break;
                    myGUS.ChangeIRQDMA = false;
                    if ((myGUS.mixControl & 0x40) != 0) {
                        // IRQ configuration, only use low bits for irq 1
                        if ((irqtable[val & 0x7]) != 0) myGUS.irq1 = irqtable[val & 0x7];
                        //Log.log_msg("Assigned GUS to IRQ " + myGUS.irq1);
                    } else {
                        // DMA configuration, only use low bits for dma 1
                        if ((dmatable[val & 0x7]) != 0) myGUS.dma1 = dmatable[val & 0x7];
                        //Log.log_msg("Assigned GUS to DMA " + myGUS.dma1);
                    }
                    break;
                case 0x302:
                    myGUS.gCurChannel = (char) (val & 31);
                    curchan = guschan[myGUS.gCurChannel];
                    break;
                case 0x303:
                    myGUS.gRegSelect = (short) val;
                    myGUS.gRegData = 0;
                    break;
                case 0x304:
                    if (iolen == 2) {
                        myGUS.gRegData = val;
                        ExecuteGlobRegister();
                    } else myGUS.gRegData = val;
                    break;
                case 0x305:
                    myGUS.gRegData = (char) ((0x00ff & myGUS.gRegData) | val << 8);
                    ExecuteGlobRegister();
                    break;
                case 0x307:
                    if (myGUS.gDramAddr < GUSRam.length) GUSRam[(int) myGUS.gDramAddr] = (byte)val;
                    break;
                default:
                    //Log.log_msg("Write GUS at port 0x" + port + " with " + val);
                    break;
            }
        }
    };
    private static DMA.DMA_CallBack GUS_DMA_Callback = new DMA.DMA_CallBack() {
        public void call(DMA.DmaChannel chan, int event) {
            if (event != DMAEvent.DMA_UNMASKED) return;
            int dmaaddr = myGUS.dmaAddr << 4;
            if ((myGUS.DMAControl & 0x2) == 0) {
                int read = chan.Read(chan.currcnt + 1, GUSRam, dmaaddr);
                //Check for 16 or 8bit channel
                read *= (chan.DMA16 + 1);
                if ((myGUS.DMAControl & 0x80) != 0) {
                    //Invert the MSB to convert twos compliment form
                    int i;
                    if ((myGUS.DMAControl & 0x40) == 0) {
                        // 8-bit data
                        for (i = dmaaddr; i < (dmaaddr + read); i++) GUSRam[i] ^= 0x80;
                    } else {
                        // 16-bit data
                        for (i = dmaaddr + 1; i < (dmaaddr + read); i += 2) GUSRam[i] ^= 0x80;
                    }
                }
            } else {
                //Read data out of UltraSound
                chan.Write(chan.currcnt + 1, GUSRam, dmaaddr);
            }
            /* Raise the TC irq if needed */
            if ((myGUS.DMAControl & 0x20) != 0) {
                myGUS.IRQStatus |= 0x80;
                GUS_CheckIRQ();
            }
            chan.Register_Callback(null);
        }
    };

    private static Mixer.MIXER_Handler GUS_CallBack = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int len) {
            //memset(&MixTemp,0,len*8);
            short[] buf16 = Mixer.MixTemp16;
            for (int i = 0; i < len * 8; i++) {
                buf16[i] = 0;
            }
            int i;
            int[] buf32 = Mixer.MixTemp32;
            int boffset = 0;
            for (int j = 0; j < buf32.length; j++) {
                int one = buf16[boffset];
                int two = buf16[boffset + 1];
                buf32[j] = one << 16 | two;
                boffset += 2;
            }

            for (i = 0; i < myGUS.ActiveChannels; i++)
                guschan[i].generateSamples(buf32, len);
            for (i = 0; i < len * 2; i++) {
                int sample = ((buf32[i] >> 13) * AutoAmp) >> 9;
                if (sample > 32767) {
                    sample = 32767;
                    AutoAmp--;
                } else if (sample < -32768) {
                    sample = -32768;
                    AutoAmp--;
                }
                buf16[i] = (short) (sample);
            }
            gus_chan.AddSamples_s16(len, buf16);
            CheckVoiceIrq();
        }
    };

    // Generate logarithmic to linear volume conversion tables
    private static void MakeTables() {
        int i;
        double out = (double) (1 << 13);
        for (i = 4095; i >= 0; i--) {
            vol16bit[i] = (char) out;
            out /= 1.002709201;        /* 0.0235 dB Steps */
        }
        pantable[0] = 4095 << RAMP_FRACT;
        for (i = 1; i < 16; i++) {
            pantable[i] = (long) (-128.0 * (Math.log((double) i / 15.0) / Math.log(2.0)) * (double) (1 << RAMP_FRACT));
        }
    }

    static private Gus test = null;
    private static Section.SectionFunction GUS_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            GUSRam = null;
            test = null;
        }
    };
    public static Section.SectionFunction GUS_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new Gus(section);
            section.AddDestroyFunction(GUS_ShutDown, true);
        }
    };
    /*
	~GUS() {
		if(!IS_EGAVGA_ARCH) return;
		Section_prop * section=static_cast<Section_prop *>(m_configuration);
		if(!section.Get_bool("gus")) return;

		myGUS.gRegData=0x1;
		GUSReset();
		myGUS.gRegData=0x0;

		for(Bitu i=0;i<32;i++) {
			delete guschan[i];
		}

		memset(&myGUS,0,sizeof(myGUS));
		memset(GUSRam,0,1024*1024);
		}
	};
     */

    static private class GusTimer {
        short value;
        boolean reached;
        boolean raiseirq;
        boolean masked;
        boolean running;
        float delay;
    }

    static private class GFGus {
        GusTimer[] timers = new GusTimer[2];
        short gRegSelect;
        char gRegData;
        long gDramAddr;
        char gCurChannel;

        short DMAControl;
        char dmaAddr;
        short TimerControl;
        short SampControl;
        short mixControl;
        short ActiveChannels;
        long basefreq;

        int rate;
        int portbase;
        short dma1;
        short dma2;

        short irq1;
        short irq2;

        boolean irqenabled;
        boolean ChangeIRQDMA;
        // IRQ status register values
        short IRQStatus;
        long ActiveMask;
        short IRQChan;
        long RampIRQ;
        long WaveIRQ;

        public GFGus() {
            for (int i = 0; i < timers.length; i++) {
                timers[i] = new GusTimer();
            }
        }
    }
}


