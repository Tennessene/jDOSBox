package jdos.host;

import jdos.host.router.EtherUtil;
import jdos.misc.setup.Section_prop;
import jdos.util.Ptr;

import java.util.LinkedList;

public class UserEthernet extends EtherUtil implements Ethernet {

    public void send(byte[] buffer, int offset, int len) {
        //dump(buffer, offset, len);
        ether.handle(buffer, offset, len);
    }

    public void receive(RxFrame frame) {
        if (frames.size()>0) {
            byte[] data = (byte[])frames.removeFirst();
            //dump(data, 0, data.length);
            frame.rx_frame(new Ptr(data, 0), data.length);
        }
    }

    public boolean open(Section_prop section, byte[] mac) {
        frames = new LinkedList();
        return true;
    }

    public void close() {
    }
}
