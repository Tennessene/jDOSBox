package jdos.host;

import jdos.misc.setup.Section_prop;

public interface Ethernet {
    void send(byte[] buffer, int offset, int len);
    void receive(RxFrame frame);
    boolean open(Section_prop section, byte[] mac);
    void close();
}
