package jdos.gui;

import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.misc.Log;
import jdos.types.LogTypes;
import jdos.types.LogSeverities;
import jdos.Dosbox;

import javax.sound.midi.*;
import java.io.InputStream;

public class Midi extends Module_base {
    static final private int SYSEX_SIZE = 1024;
    static final private int RAWBUF	= 1024;

    static final private byte[] MIDI_evt_len = new byte[] {
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x00
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x10
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x20
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x30
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x40
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x50
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x60
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x70

      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0x80
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0x90
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xa0
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xb0

      2,2,2,2, 2,2,2,2, 2,2,2,2, 2,2,2,2,  // 0xc0
      2,2,2,2, 2,2,2,2, 2,2,2,2, 2,2,2,2,  // 0xd0

      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xe0

      0,2,3,2, 0,0,1,0, 1,0,1,1, 1,0,1,0   // 0xf0
    };

    static private class _midi {
        int status;
        int cmd_len;
        int cmd_pos;
        byte[] cmd_buf = new byte[8];
        byte[] rt_buf = new byte[8];
        public static class Sysex {
            byte[] buf = new byte[SYSEX_SIZE];
            int used;
        }
        public Sysex sysex = new Sysex();
        Receiver handler;
        MidiDevice device;
    }
    static final private _midi midi = new _midi();
    static private ShortMessage msg = new ShortMessage();
    static private SysexMessage sysex_msg = new SysexMessage();

    static public void MIDI_RawOutByte(/*Bit8u*/int data) {
        /* Test for a realtime MIDI message */
        if (data>=0xf8) {
            try {msg.setMessage(data);} catch (Exception e) {}
            midi.handler.send(msg, -1);
            return;
        }
        /* Test for a active sysex tranfer */
        if (midi.status==0xf0) {
            if ((data&0x80)==0) {
                if (midi.sysex.used<(SYSEX_SIZE-1)) midi.sysex.buf[midi.sysex.used++]=(byte)data;
                return;
            } else {
                midi.sysex.buf[midi.sysex.used++]=(byte)0xf7;
                try {sysex_msg.setMessage(midi.sysex.buf, midi.sysex.used);} catch (Exception e) {}
                midi.handler.send(sysex_msg, -1);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_ALL, LogSeverities.LOG_NORMAL,"Sysex message size "+midi.sysex.used);
//                if (CaptureState & CAPTURE_MIDI) {
//                    CAPTURE_AddMidi( true, midi.sysex.used-1, &midi.sysex.buf[1]);
//                }
            }
        }
        if ((data&0x80)!=0) {
            midi.status=data;
            midi.cmd_pos=0;
            midi.cmd_len=MIDI_evt_len[data];
            if (midi.status==0xf0) {
                midi.sysex.buf[0]=(byte)0xf0;
                midi.sysex.used=1;
            }
        }
        if (midi.cmd_len!=0) {
            midi.cmd_buf[midi.cmd_pos++]=(byte)data;
            if (midi.cmd_pos >= midi.cmd_len) {
//                if (CaptureState & CAPTURE_MIDI) {
//                    CAPTURE_AddMidi(false, midi.cmd_len, midi.cmd_buf);
//                }
                try {msg.setMessage(midi.cmd_buf[0], midi.cmd_buf[1], midi.cmd_buf[2]);} catch (Exception e) {}
                midi.handler.send(msg, -1);
                midi.cmd_pos=1;		//Use Running status
            }
        }
    }

    static public boolean MIDI_Available()  {
        return midi.device != null;
    }
    
    public Midi(Section configuration) {
        super(configuration);
        Section_prop section=(Section_prop)configuration;
		String dev=section.Get_string("mididevice");
		//const char * conf=section->Get_string("midiconfig");
		/* If device = "default" go for first handler that works */
//		MAPPER_AddHandler(MIDI_SaveRawEvent,MK_f8,MMOD1|MMOD2,"caprawmidi","Cap MIDI");
		midi.status=0x00;
		midi.cmd_pos=0;
		midi.cmd_len=0;
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        boolean def = dev.equalsIgnoreCase("default");

        if (!def) {
            for (int i=0;i<devices.length;i++) {
                if (devices[i].getName().equalsIgnoreCase(dev)) {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(devices[0]);
                        device.open();
                        midi.handler =  device.getReceiver();
                        midi.device = device;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (midi.handler==null) {
                Log.log_msg("MIDI:Can't find device:"+dev+", finding default handler.");
            }
        }
        if (midi.handler == null) {
            Synthesizer	synth = null;
            Soundbank soundbank = null;

            try {
                synth=MidiSystem.getSynthesizer();
            } catch (Exception e) {
            }
            if (synth != null) {
                String fileName = "default";
                soundbank = synth.getDefaultSoundbank();
                if (soundbank == null) {
                    fileName = "soundbank-deluxe.gm";
                    InputStream is = Dosbox.class.getResourceAsStream(fileName);
                    if (is == null) {
                        fileName = "soundbank-mid.gm";
                        is = Dosbox.class.getResourceAsStream("soundbank-mid.gm");
                    }
                    if (is == null) {
                        fileName = "soundbank-min.gm";
                        is = Dosbox.class.getResourceAsStream("soundbank-min.gm");
                    }
                    if (is != null) {
                        try {
                            soundbank = MidiSystem.getSoundbank(is);
                        } catch (Exception e) {
                        }
                        try {is.close();} catch (Exception e) {}
                    }
                }
                if (soundbank != null) {
                    try {
                        synth.open();
                        if (synth.isSoundbankSupported(soundbank) && synth.loadAllInstruments(soundbank)) {
                            Log.log_msg("Using Soundbank: "+fileName);
                            midi.handler = synth.getReceiver();
                            midi.device = synth;
                        } else {
                            synth.close();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        for (int i=0;i<devices.length && midi.handler == null;i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
                device.open();
                midi.handler =  device.getReceiver();
                midi.device = device;
                Log.log_msg("MIDI:Opened device:"+devices[i].getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Midi test;

    public static Section.SectionFunction MIDI_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            if(midi.device!=null) {
                midi.handler.close();
                midi.device.close();
            }
            test = null;
        }
    };

    public static Section.SectionFunction MIDI_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new Midi(section);
            section.AddDestroyFunction(MIDI_Destroy);
        }
    };
}
