package jdos.util;

import jdos.hardware.Memory;

public class HostPtr extends Ptr {
    public HostPtr(int size) {
        super(Memory.allocate(size));
    }
}
