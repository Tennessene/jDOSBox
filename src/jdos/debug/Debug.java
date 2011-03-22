package jdos.debug;

import jdos.misc.setup.Section;

public class Debug { 
    static public /*Bitu*/int cycle_count;
    static public /*Bitu*/int debugCallback;
    static public void DEBUG_HeavyWriteLogInstruction() {

    }
    static public boolean DEBUG_IntBreakpoint(/*Bit8u*/short intNum) {
        return false;
    }
    static public boolean DEBUG_HeavyIsBreakpoint() {
        return false;
    }
    static public boolean DEBUG_Breakpoint() {
        return false;
    }

    public static Section.SectionFunction DEBUG_Init = new Section.SectionFunction() {
        public void call(Section section) {
        }
    };

    public static boolean DEBUG_ExitLoop() {
        return false;
    }
}
