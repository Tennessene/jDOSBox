package jdos.hardware;

import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;

public class Disney extends Module_base {
    public Disney(Section configuration) {
        super(configuration);
    }
    public static final Section.SectionFunction DISNEY_Init = section -> {
    };
}
