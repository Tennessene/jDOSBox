package jdos.hardware;

import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;

public class Gus extends Module_base {
    public static /*Bit8u*/short adlib_commandreg;
    public Gus(Section configuration) {
        super(configuration);
    }
    public static Section.SectionFunction GUS_Init = new Section.SectionFunction() {
        public void call(Section section) {
        }
    };
}
