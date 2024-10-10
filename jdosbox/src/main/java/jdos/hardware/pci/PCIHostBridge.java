package jdos.hardware.pci;

import jdos.hardware.Memory;
import jdos.util.IntRef;

public class PCIHostBridge extends PCI_Device {
    public PCIHostBridge(){
        super(0x8086, 0x1237);
        PCI.pci_interface.RegisterPCIDevice(this, 0);

        int pci_routing=0xf9000;
        Memory.phys_writed(pci_routing + 0x00, 0x52495024);		// signature
        Memory.phys_writew(pci_routing + 0x04, 0x0100);			// version
        Memory.phys_writew(pci_routing + 0x06, 32 + (6 * 16));	// table size
        Memory.phys_writeb(pci_routing + 0x08, 0x00);				// router bus
        Memory.phys_writeb(pci_routing + 0x09, 0x08);				// router dev func
        Memory.phys_writew(pci_routing + 0x0a, 0x0000);			// exclusive IRQs
        Memory.phys_writew(pci_routing + 0x0c, 0x8086);			// vendor ID
        Memory.phys_writew(pci_routing + 0x0e, 0x7000);			// device ID
        Memory.phys_writew(pci_routing + 0x10, 0x0000);			// miniport data
        Memory.phys_writew(pci_routing + 0x12, 0x0000);			// miniport data
        Memory.phys_writeb(pci_routing + 0x1f, 0x07);				// checksum

        pci_routing+=0x20;
        for (int i=0; i<6; i++) {
            Memory.phys_writeb(pci_routing + 0x00, 0x00);				// bus number
            Memory.phys_writeb(pci_routing + 0x01, 0x08 + i * 8);			// device number
            pci_routing+=0x02;
            for (int link=0; link<4; link++) {
                Memory.phys_writeb(pci_routing + 0x00, 0x60 + ((i + link) & 3));	// link value
                Memory.phys_writew(pci_routing + 0x01, 0xdef8);				// bitmap
                pci_routing+=0x03;
            }
            Memory.phys_writeb(pci_routing + 0x00, i);				// slot
            Memory.phys_writeb(pci_routing + 0x01, 0x00);
            pci_routing+=0x02;
        }

        Memory.phys_writeb(pci_routing + 0x00, 11);		// irq
        Memory.phys_writeb(pci_routing + 0x01, 10);		// irq
        Memory.phys_writeb(pci_routing + 0x02, 9);		// irq
        Memory.phys_writeb(pci_routing + 0x03, 5);		// irq
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
