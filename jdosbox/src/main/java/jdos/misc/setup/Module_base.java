package jdos.misc.setup;

public class Module_base {
    /* Base for all hardware and software "devices" */
    protected Section m_configuration;
    public Module_base(Section configuration) {
        m_configuration = configuration;
    }
    public boolean Change_Config(Section section) {
        return false;
    }
}
