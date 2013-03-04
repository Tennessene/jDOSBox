package jdos.gui;

import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;

public class Midi extends Module_base {

    static public void MIDI_RawOutByte(/*Bit8u*/int data) {
    }

    static public boolean MIDI_Available() {
        return false;
    }

    public Midi(Section configuration) {
        super(configuration);
    }

    public static Section.SectionFunction MIDI_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
        }
    };

    public static Section.SectionFunction MIDI_Init = new Section.SectionFunction() {
        public void call(Section section) {
            section.AddDestroyFunction(MIDI_Destroy);
        }
    };
}
