package jdos.hardware;

import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;

public class IPX extends Module_base {
    public IPX(Section configuration) {
        super(configuration);
    }
    public static Section.SectionFunction IPX_Init = new Section.SectionFunction() {
        public void call(Section section) {
        }
    };
}
