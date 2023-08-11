package jdos.dos;

import jdos.util.IntRef;
import jdos.util.LongRef;

public class DOS_Device extends DOS_File {
    public DOS_Device(DOS_Device orig) {
        super(orig);
        devnum=orig.devnum;
        open=true;
    }
    public DOS_Device() {
    }

    public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
        return Dos_devices.Devices[devnum].Read(data,size);
    }
    public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
        return Dos_devices.Devices[devnum].Write(data,size);
    }
    public boolean Seek(/*Bit32u*/LongRef pos,/*Bit32u*/int type) {
        return Dos_devices.Devices[devnum].Seek(pos,type);
    }
    public boolean Close() {
        return Dos_devices.Devices[devnum].Close();
    }
    public /*Bit16u*/int GetInformation() {
        return Dos_devices.Devices[devnum].GetInformation();
    }
    public boolean ReadFromControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
        return Dos_devices.Devices[devnum].ReadFromControlChannel(bufptr,size,retcode);
    }
    public boolean WriteToControlChannel(/*PhysPt*/int bufptr,/*Bit16u*/int size,/*Bit16u*/IntRef retcode) {
        return Dos_devices.Devices[devnum].WriteToControlChannel(bufptr,size,retcode);
    }
    void SetDeviceNumber(/*Bitu*/int num) { devnum=num;}
    private /*Bitu*/int devnum=0;
}
