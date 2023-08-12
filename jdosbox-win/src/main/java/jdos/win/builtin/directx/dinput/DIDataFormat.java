package jdos.win.builtin.directx.dinput;

import jdos.hardware.Memory;

public class DIDataFormat {
    public DIDataFormat(int address) {
        dwSize = Memory.mem_readd(address);address+=4;
        dwObjSize = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
        dwDataSize = Memory.mem_readd(address);address+=4;
        dwNumObjs = Memory.mem_readd(address);address+=4;
        address = Memory.mem_readd(address);
        rgodf = new DIObjectDataFormat[dwNumObjs];
        for (int i=0;i<dwNumObjs;i++) {
            rgodf[i] = new DIObjectDataFormat(address);
            address+=DIObjectDataFormat.SIZE;
        }
    }
    public int dwSize;
    public int dwObjSize;
    public int dwFlags;
    public int dwDataSize;
    public int dwNumObjs;
    public DIObjectDataFormat[] rgodf;
}
