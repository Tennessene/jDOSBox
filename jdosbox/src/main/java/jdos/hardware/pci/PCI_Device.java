package jdos.hardware.pci;

import jdos.misc.Log;
import jdos.util.IntRef;

/**
* Created with IntelliJ IDEA.
* User: James
* Date: 1/31/13
* Time: 5:46 AM
* To change this template use File | Settings | File Templates.
*/
public abstract class PCI_Device {
    protected /*Bits*/int pci_id, pci_subfunction;
    private /*Bit16u*/int vendor_id, device_id;
    protected PCI_BAR[] bars = new PCI_BAR[6];

    // subdevices declarations, they will respond to pci functions 1 to 7
    // (main device is attached to function 0)
    private /*Bitu*/int num_subdevices;
    private PCI_Device[] subdevices = new PCI_Device[PCI.PCI_MAX_PCIFUNCTIONS-1];

    public PCI_Device(/*Bit16u*/int vendor, /*Bit16u*/int device) {
        pci_id=-1;
        pci_subfunction=-1;
        vendor_id=vendor;
        device_id=device;
        num_subdevices=0;
    }

    public void setBAR(PCI_BAR bar, int index) {
        this.bars[index] = bar;
    }

    public /*Bits*/int PCIId() {
        return pci_id;
    }
    public /*Bits*/int PCISubfunction() {
        return pci_subfunction;
    }
    public /*Bit16u*/int VendorID() {
        return vendor_id;
    }
    public /*Bit16u*/int DeviceID() {
        return device_id;
    }

    public void SetPCIId(/*Bitu*/int number, /*Bits*/int subfct) {
        if ((number>=0) && (number< PCI.PCI_MAX_PCIDEVICES)) {
        pci_id=number;
        if ((subfct>=0) && (subfct< PCI.PCI_MAX_PCIFUNCTIONS-1))
            pci_subfunction=subfct;
        else
            pci_subfunction=-1;
    }
    }

    public boolean AddSubdevice(PCI_Device dev) {
        if (num_subdevices< PCI.PCI_MAX_PCIFUNCTIONS-1) {
            if (subdevices[num_subdevices]!=null) Log.exit("PCI subdevice slot already in use!");
            subdevices[num_subdevices]=dev;
            num_subdevices++;
            return true;
        }
        return false;
    }

    public void RemoveSubdevice(/*Bits*/int subfct) {
        if ((subfct>0) && (subfct< PCI.PCI_MAX_PCIFUNCTIONS)) {
            if (subfct<=NumSubdevices()) {
                //subdevices[subfct-1].destroy();
                subdevices[subfct-1]=null;
                // should adjust things like num_subdevices as well...
            }
        }
    }

    public PCI_Device GetSubdevice(/*Bits*/int subfct) {
        if (subfct>= PCI.PCI_MAX_PCIFUNCTIONS) return null;
        if (subfct>0) {
            if (subfct<=NumSubdevices()) return subdevices[subfct-1];
        } else if (subfct==0) {
            return this;
        }
        return null;
    }

    public /*Bit16u*/int NumSubdevices() {
        if (num_subdevices> PCI.PCI_MAX_PCIFUNCTIONS-1) return PCI.PCI_MAX_PCIFUNCTIONS-1;
        return num_subdevices;
    }

    public /*Bits*/int GetNextSubdeviceNumber() {
        if (num_subdevices>= PCI.PCI_MAX_PCIFUNCTIONS-1) return -1;
        return num_subdevices+1;
    }

    public int getBAR(int index, int currentValue) {
        if (bars[index]==null)
            return 0;
        return bars[index].getBAR(currentValue);
    }

    public void setBAR(int index, int newValue) {
        if (bars[index]!=null)
            bars[index].setBAR(newValue);
    }

    public abstract /*Bits*/int ParseReadRegister(/*Bit8u*/int regnum);
    public abstract boolean OverrideReadRegister(/*Bit8u*/int regnum, /*Bit8u**/IntRef rval, /*Bit8u**/IntRef rval_mask);
    public abstract /*Bits*/int ParseWriteRegister(/*Bit8u*/int regnum,/*Bit8u*/int value);
    public abstract boolean InitializeRegisters(/*Bit8u*/byte[] registers);
}
