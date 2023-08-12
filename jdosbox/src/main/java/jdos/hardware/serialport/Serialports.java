package jdos.hardware.serialport;

import jdos.ints.Bios;
import jdos.misc.Log;
import jdos.misc.setup.*;

public class Serialports extends Module_base {
    static public Serial[] serialports = new Serial[4];
    static final private int[] serial_baseaddr = {0x3f8,0x2f8,0x3e8,0x2e8};
    static Serialports testSerialPortsBaseclass;

    public static Section.SectionFunction SERIAL_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            for (/*Bitu*/int i = 0; i < 4; i++)
                if (serialports[i]!= null) {
                    serialports[i] = null;
                }
            testSerialPortsBaseclass = null;
        }
    };

    public Serialports(Section configuration) {
        super(configuration);
        /*Bit16u*/int[] biosParameter = new int[4];
		Section_prop section = (Section_prop)configuration;

		for(/*Bitu*/int i = 0; i < 4; i++) {
			// get the configuration property
			String s_property = "serial"+(char)((int)'1' + i);
			Prop_multival p = section.Get_multival(s_property);
			String type = p.GetSection().Get_string("type");
			CommandLine cmd = new CommandLine(null,p.GetSection().Get_string("parameters"));

			// detect the type
//			if (type.equals("dummy")) {
//				serialports[i] = new CSerialDummy (i, cmd);
//			} else if (type.equals("directserial")) {
//				serialports[i] = new CDirectSerial (i, cmd);
//				if (!serialports[i].InstallationSuccessful)  {
//					// serial port name was wrong or already in use
//					serialports[i] = null;
//				}
//			} else if(type.equals("modem")) {
//				serialports[i] = new CSerialModem (i, &cmd);
//				if (!serialports[i].InstallationSuccessful)  {
//					serialports[i] = null;
//				}
//			} else if(type.equals("nullmodem")) {
//				serialports[i] = new CNullModem (i, &cmd);
//				if (!serialports[i].InstallationSuccessful)  {
//					serialports[i] = null;
//				}
//			} else
            if(type.equals("disabled")) {
				serialports[i] = null;
			} else {
				serialports[i] = null;
				Log.log_msg("Invalid type for serial"+(i+1));
			}
			if(serialports[i]!=null) biosParameter[i] = serial_baseaddr[i];
		} // for 1-4
		Bios.BIOS_SetComPorts (biosParameter);
    }

    public static Section.SectionFunction SERIAL_Init = new Section.SectionFunction() {
        public void call(Section section) {
            // should never happen
            testSerialPortsBaseclass = new Serialports(section);
            section.AddDestroyFunction(SERIAL_Destroy, true);
        }
    };
}
