package jdos.host;

import jdos.misc.setup.Section_prop;

public interface Ethernet {
    public void send(byte[] buffer, int offset, int len);
    public void receive(RxFrame frame);
    public boolean open(Section_prop section, byte[] mac);
    public void close();
}
