package jdos.host;

import jdos.util.Ptr;

public interface RxFrame {
    public boolean rx_frame(Ptr buf, /*unsigned*/int io_len);
}
