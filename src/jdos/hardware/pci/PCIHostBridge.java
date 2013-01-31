package jdos.hardware.pci;

import jdos.util.IntRef;

public class PCIHostBridge extends PCI_Device {
    public PCIHostBridge(){
        super(0x8086, 0x1237);
        PCI.pci_interface.RegisterPCIDevice(this, 0);
    }

    public int ParseReadRegister(int regnum) {
        return regnum;
    }

    public boolean OverrideReadRegister(int regnum, IntRef rval, IntRef rval_mask) {
        return false;
    }

    public int ParseWriteRegister(int regnum, int value) {
        return value;
    }

    public boolean InitializeRegisters(byte[] registers) {
        registers[0x08] = 0x02; // revision
        registers[0x0a] = 0x00;
        registers[0x0b] = 0x06;  // class, pci host bridge
        registers[0x0e] = 0x00; // header type

        // reset
        registers[0x04] = 0x06;
        registers[0x05] = 0x00;
        registers[0x06] = (byte)0x80;
        registers[0x07] = 0x02;
        registers[0x0d] = 0x00;
        registers[0x0f] = 0x00;
        registers[0x50] = 0x00;
        registers[0x51] = 0x01;
        registers[0x52] = 0x00;
        registers[0x53] = (byte)0x80;
        registers[0x54] = 0x00;
        registers[0x55] = 0x00;
        registers[0x56] = 0x00;
        registers[0x57] = 0x01;
        registers[0x58] = 0x10;
        return true;
    }
}
