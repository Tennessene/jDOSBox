package jdos.hardware.pci;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;

public class PCI extends Module_base {
    static final int PCI_MAX_PCIDEVICES = 10;
    static final int PCI_MAX_PCIFUNCTIONS = 8;

    static public boolean PCI_IsInitialized() {
        if (pci_interface!=null) return pci_interface.IsInitialized();
	    return false;
    }

    static public /*RealPt*/int PCI_GetPModeInterface() {
        if (pci_interface!=null) {
		    return pci_interface.GetPModeCallbackPointer();
	    }
	    return 0;
    }

    private static /*Bit32u*/int pci_caddress=0;			// current PCI addressing
    private static /*Bitu*/int pci_devices_installed=0;	// number of registered PCI devices

    private static /*Bit8u*/byte[][][] pci_cfg_data = new byte[PCI_MAX_PCIDEVICES][PCI_MAX_PCIFUNCTIONS][256];		// PCI configuration data
    private static PCI_Device[] pci_devices =new PCI_Device[PCI_MAX_PCIDEVICES];		// registered PCI devices

    // PCI address
    // 31    - set for a PCI access
    // 30-24 - 0
    // 23-16 - bus number			(0x00ff0000)
    // 15-11 - device number (slot)	(0x0000f800)
    // 10- 8 - subfunction number	(0x00000700)
    //  7- 2 - config register #	(0x000000fc)
    static private final IoHandler.IO_WriteHandler write_pci_addr = new IoHandler.IO_WriteHandler() {
        public void call(int port, int val, int iolen) {
            if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PCI, LogSeverities.LOG_NORMAL,"Write PCI address :="+Integer.toString(val, 16));
            pci_caddress=val;
        }
    };

    static void write_pci_register(PCI_Device dev,/*Bit8u*/int regnum,/*Bit8u*/int value) {
        // vendor/device/class IDs/header type/etc. are read-only
        if ((regnum<0x04) || ((regnum>=0x06) && (regnum<0x0c)) || (regnum==0x0e)) return;
        if (dev==null) return;
        switch (pci_cfg_data[dev.PCIId()][dev.PCISubfunction()][0x0e]&0x7f) {	// header-type specific handling
            case 0x00:
                if ((regnum>=0x28) && (regnum<0x30)) return;	// subsystem information is read-only
                break;
            case 0x01:
            case 0x02:
            default:
                break;
        }

        // call device routine for special actions and the
        // possibility to discard/replace the value that is to be written
        /*Bits*/int parsed_register=dev.ParseWriteRegister(regnum,value);
        if (parsed_register>=0) {
            pci_cfg_data[dev.PCIId()][dev.PCISubfunction()][regnum]=(/*Bit8u*/byte)(parsed_register&0xff);

            if (regnum>=0x10 && regnum<0x28 && (regnum % 4)==3) {
                int index = regnum / 4 * 4;
                byte[] config = pci_cfg_data[dev.PCIId()][dev.PCISubfunction()];
                int newBAR = (config[index] & 0xFF) | ((config[index+1] & 0xFF) << 8) | ((config[index+2] & 0xFF) << 16) | ((config[index+3] & 0xFF) << 24);
                if (newBAR!=-1)
                    dev.setBAR((regnum-0x10)/4, newBAR);
            }
        }
    }

    static private final IoHandler.IO_WriteHandler write_pci = new IoHandler.IO_WriteHandler() {
        public void call(int port, int val, int iolen) {
            if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PCI, LogSeverities.LOG_NORMAL,"Write PCI data :="+Integer.toString(val, 16)+" (len "+iolen+")");

            // check for enabled/bus 0
            if ((pci_caddress & 0x80ff0000) == 0x80000000) {
                int devnum = (pci_caddress >> 11) & 0x1f;
                int fctnum = (pci_caddress >> 8) & 0x7;
                int regnum = (pci_caddress & 0xfc) + (port & 0x03);
                if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PCI, LogSeverities.LOG_NORMAL,"  Write to device "+Integer.toString(devnum, 16)+" register "+Integer.toString(regnum, 16)+" (function "+Integer.toString(fctnum, 16)+") (:="+Integer.toString(val, 16)+")");

                if (devnum>=pci_devices_installed) return;
                PCI_Device masterdev=pci_devices[devnum];
                if (masterdev==null) return;
                if (fctnum>masterdev.NumSubdevices()) return;

                PCI_Device dev=masterdev.GetSubdevice(fctnum);
                if (dev==null) return;
                System.out.println("PCI 0x"+Integer.toHexString(dev.PCIId())+" write "+Integer.toHexString(regnum)+":"+Integer.toHexString(val));
                // write data to PCI device/configuration
                switch (iolen) {
                    case 1: write_pci_register(dev,regnum+0,(val&0xff)); break;
                    case 2: write_pci_register(dev,regnum+0,(val&0xff));
                            write_pci_register(dev,regnum+1,((val>>8)&0xff)); break;
                    case 4: write_pci_register(dev,regnum+0,(val&0xff));
                            write_pci_register(dev,regnum+1,((val>>8)&0xff));
                            write_pci_register(dev,regnum+2,((val>>16)&0xff));
                            write_pci_register(dev,regnum+3,((val>>24)&0xff)); break;
                }
            }
        }
    };

    static private final IoHandler.IO_ReadHandler read_pci_addr = new IoHandler.IO_ReadHandler() {
        public int call(int port, int iolen) {
            if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PCI, LogSeverities.LOG_NORMAL,"Read PCI address . "+Integer.toString(pci_caddress, 16));
            return pci_caddress;
        }
    };

    // read single 8bit value from register file (special register treatment included)
    static /*Bit8u*/int read_pci_register(PCI_Device dev,/*Bit8u*/int regnum) {
        byte[] config = pci_cfg_data[dev.PCIId()][dev.PCISubfunction()];
        switch (regnum) {
            case 0x00:
                return (dev.VendorID()&0xff);
            case 0x01:
                return ((dev.VendorID()>>8)&0xff);
            case 0x02:
                return (dev.DeviceID()&0xff);
            case 0x03:
                return ((dev.DeviceID()>>8)&0xff);
            case 0x0e:
                return (config[regnum]&0x7f) | ((dev.NumSubdevices()>0)?0x80:0x00);
            default:
                break;
        }

        if (regnum>=0x10 && regnum<0x28) {
            int index = regnum / 4 * 4;
            int value = (config[index] & 0xFF) | ((config[index+1] & 0xFF) << 8) | ((config[index+2] & 0xFF) << 16) | ((config[index+3] & 0xFF) << 24);
            value = dev.getBAR((regnum-0x10)/4, value);
            return (value >>> ((regnum % 4)*8)) & 0xFF;
        }

        // call device routine for special actions and possibility to discard/remap register
        /*Bits*/int parsed_regnum=dev.ParseReadRegister(regnum);
        if ((parsed_regnum>=0) && (parsed_regnum<256))
            return config[parsed_regnum];

        /*Bit8u*/IntRef newval = new IntRef(0), mask = new IntRef(0);
        if (dev.OverrideReadRegister(regnum, newval, mask)) {
            /*Bit8u*/int oldval=config[regnum] & (~mask.value);
            return oldval | (newval.value & mask.value);
        }

        return 0xff;
    }

    static private final IoHandler.IO_ReadHandler read_pci = new IoHandler.IO_ReadHandler() {
        public int call(int port, int iolen) {
            if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PCI, LogSeverities.LOG_NORMAL,"Read PCI data . "+Integer.toString(pci_caddress, 16));

            if ((pci_caddress & 0x80ff0000) == 0x80000000) {
                /*Bit8u*/int devnum = (pci_caddress >> 11) & 0x1f;
                /*Bit8u*/int fctnum = (pci_caddress >> 8) & 0x7;
                /*Bit8u*/int regnum = (pci_caddress & 0xfc) + (port & 0x03);
                if (devnum>=pci_devices_installed) return 0xffffffff;
                if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_PCI, LogSeverities.LOG_NORMAL,"  Read from device "+Integer.toString(devnum, 16)+" register "+Integer.toString(regnum, 16)+" (function "+Integer.toString(fctnum, 16)+"); addr "+Integer.toString(pci_caddress, 16));

                PCI_Device masterdev=pci_devices[devnum];
                if (masterdev==null) return 0xffffffff;
                if (fctnum>masterdev.NumSubdevices()) return 0xffffffff;

                PCI_Device dev=masterdev.GetSubdevice(fctnum);
                if (regnum>3) {
                    int ii=0;
                }
                if (dev!=null) {
                    switch (iolen) {
                        case 1:
                            {
                                int val8=read_pci_register(dev,regnum) & 0xFF;
                                System.out.println("PCI 0x"+Integer.toHexString(dev.PCIId())+" read1 "+Integer.toHexString(regnum)+":"+Integer.toHexString(val8));
                                return val8;
                            }
                        case 2:
                            {
                                int val16=read_pci_register(dev,regnum) & 0xFFFF;
                                val16|=(read_pci_register(dev,regnum+1)<<8);
                                System.out.println("PCI 0x"+Integer.toHexString(dev.PCIId())+" read2 "+Integer.toHexString(regnum)+":"+Integer.toHexString(val16));
                                return val16;
                            }
                        case 4:
                            {
                                int val32=read_pci_register(dev,regnum);
                                val32|=(read_pci_register(dev,regnum+1)<<8);
                                val32|=(read_pci_register(dev,regnum+2)<<16);
                                val32|=(read_pci_register(dev,regnum+3)<<24);
                                System.out.println("PCI 0x"+Integer.toHexString(dev.PCIId())+" read4 "+Integer.toHexString(regnum)+":"+Integer.toHexString(val32));
                                return val32;
                            }
                        default:
                            break;
                    }
                }
            }
            return 0xffffffff;
        }
    };

    static final private Callback.Handler PCI_PM_Handler = new Callback.Handler() {
        public int call() {
            Log.log_msg("PCI PMode handler, function " + Integer.toString(CPU_Regs.reg_eax.word(), 16));
            return Callback.CBRET_NONE;
        }

        public String getName() {
            return "PCI_PM_Handler";
        }
    };

    // queued devices (PCI device registering requested before the PCI framework was initialized)
    private static final int max_rqueued_devices=16;
    private static int num_rqueued_devices=0;
    private static PCI_Device[] rqueued_devices = new PCI_Device[max_rqueued_devices];

    static public PCI pci_interface=null;

	private boolean initialized;

	protected IoHandler.IO_WriteHandleObject[] PCI_WriteHandler = new IoHandler.IO_WriteHandleObject[5];
	protected IoHandler.IO_ReadHandleObject[] PCI_ReadHandler = new IoHandler.IO_ReadHandleObject[5];

	protected Callback callback_pci = new Callback();

	public /*PhysPt*/int GetPModeCallbackPointer() {
		return Memory.Real2Phys(callback_pci.Get_RealPointer());
	}

	public boolean IsInitialized() {
		return initialized;
	}

	// set up port handlers and configuration data
	public void InitializePCI() {
		// install PCI-addressing ports
		PCI_WriteHandler[0].Install(0xcf8,write_pci_addr,IoHandler.IO_MD);
		PCI_ReadHandler[0].Install(0xcf8,read_pci_addr,IoHandler.IO_MD);
		// install PCI-register read/write handlers
		for (int ct=0;ct<4;ct++) {
			PCI_WriteHandler[1+ct].Install(0xcfc+ct,write_pci,IoHandler.IO_MB);
			PCI_ReadHandler[1+ct].Install(0xcfc+ct,read_pci,IoHandler.IO_MB);
		}

		for (int dev=0; dev<PCI_MAX_PCIDEVICES; dev++)
			for (int fct=0; fct<PCI_MAX_PCIFUNCTIONS-1; fct++)
				for (int reg=0; reg<256; reg++)
					pci_cfg_data[dev][fct][reg] = 0;

		callback_pci.Install(PCI_PM_Handler,Callback.CB_IRETD,"PCI PM");

		initialized=true;
	}

	// register PCI device to bus and setup data
    public int RegisterPCIDevice(PCI_Device device) {
        return RegisterPCIDevice(device, -1);
    }

	public int RegisterPCIDevice(PCI_Device device, int slot) {
		if (device==null) return -1;

		if (slot>=0) {
			// specific slot specified, basic check for validity
			if (slot>=PCI_MAX_PCIDEVICES) return -1;
		} else {
			// auto-add to new slot, check if one is still free
			if (pci_devices_installed>=PCI_MAX_PCIDEVICES) return -1;
		}

		if (!initialized) InitializePCI();

		if (slot<0) slot=pci_devices_installed;	// use next slot
		int subfunction=0;	// main device unless specific already-occupied slot is requested
		if (pci_devices[slot]!=null) {
			subfunction=pci_devices[slot].GetNextSubdeviceNumber();
			if (subfunction<0) Log.exit("Too many PCI subdevices!");
		}

		if (device.InitializeRegisters(pci_cfg_data[slot][subfunction])) {
			device.SetPCIId(slot, subfunction);
			if (pci_devices[slot]==null) {
				pci_devices[slot]=device;
				pci_devices_installed++;
			} else {
				pci_devices[slot].AddSubdevice(device);
			}

			return slot;
		}

		return -1;
	}

	public void Deinitialize() {
		initialized=false;
		pci_devices_installed=0;
		num_rqueued_devices=0;
		pci_caddress=0;

		for (int dev=0; dev<PCI_MAX_PCIDEVICES; dev++)
			for (int fct=0; fct<PCI_MAX_PCIFUNCTIONS-1; fct++)
				for (int reg=0; reg<256; reg++)
					pci_cfg_data[dev][fct][reg] = 0;

		// install PCI-addressing ports
		PCI_WriteHandler[0].Uninstall();
		PCI_ReadHandler[0].Uninstall();
		// install PCI-register read/write handlers
		for (int ct=0;ct<4;ct++) {
			PCI_WriteHandler[1+ct].Uninstall();
			PCI_ReadHandler[1+ct].Uninstall();
		}

		callback_pci.Uninstall();
	}

	public void RemoveDevice(/*Bit16u*/int vendor_id, /*Bit16u*/int device_id) {
		for (int dct=0;dct<pci_devices_installed;dct++) {
			if (pci_devices[dct]!=null) {
				if (pci_devices[dct].NumSubdevices()>0) {
					for (int sct=1;sct<PCI_MAX_PCIFUNCTIONS;sct++) {
						PCI_Device sdev=pci_devices[dct].GetSubdevice(sct);
						if (sdev!=null) {
							if ((sdev.VendorID()==vendor_id) && (sdev.DeviceID()==device_id)) {
								pci_devices[dct].RemoveSubdevice(sct);
							}
						}
					}
				}

				if ((pci_devices[dct].VendorID()==vendor_id) && (pci_devices[dct].DeviceID()==device_id)) {
					//pci_devices[dct].destroy();
					pci_devices[dct]=null;
				}
			}
		}

		// check if all devices have been removed
		boolean any_device_left=false;
		for (int dct=0;dct<PCI_MAX_PCIDEVICES;dct++) {
			if (dct>=pci_devices_installed) break;
			if (pci_devices[dct]!=null) {
				any_device_left=true;
				break;
			}
		}
		if (!any_device_left) Deinitialize();

		int last_active_device=PCI_MAX_PCIDEVICES;
		for (int dct=0;dct<PCI_MAX_PCIDEVICES;dct++) {
			if (pci_devices[dct]!=null) last_active_device=dct;
		}
		if (last_active_device<pci_devices_installed)
			pci_devices_installed=last_active_device+1;
	}

	private PCI(Section configuration) {
        super(configuration);
		initialized=false;

        for (int i=0;i<PCI_WriteHandler.length;i++) {
            PCI_WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
        }
        for (int i=0;i<PCI_ReadHandler.length;i++) {
            PCI_ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        }

		pci_devices_installed=0;

		for (int devct=0;devct<PCI_MAX_PCIDEVICES;devct++)
			pci_devices[devct]=null;

		if (num_rqueued_devices>0) {
			// register all devices that have been added before the PCI bus was instantiated
			for (int dct=0;dct<num_rqueued_devices;dct++) {
				this.RegisterPCIDevice(rqueued_devices[dct]);
			}
			num_rqueued_devices=0;
		}
	}

    private void destroy() {
        initialized=false;
		pci_devices_installed=0;
		num_rqueued_devices=0;
    }

    public static Section.SectionFunction PCI_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            pci_interface.destroy();
            pci_interface=null;
        }
    };

    public static Section.SectionFunction PCI_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            Section_prop section = (Section_prop)sec;
            if (section.Get_bool("enabled")) {
                pci_interface = new PCI(sec);
                new PCIHostBridge();
                //new PCIIsaBridge();
                sec.AddDestroyFunction(PCI_ShutDown,false);
            }
        }
    };
}
