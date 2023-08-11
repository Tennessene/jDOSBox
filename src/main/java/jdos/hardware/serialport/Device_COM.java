package jdos.hardware.serialport;

public class Device_COM {
//        extends DOS_Device {
//public:
//	// Creates a COM device that communicates with the num-th parallel port, i.e. is LPTnum
//	device_COM(class CSerial* sc) {
//        sclass = sc;
//	    SetName(serial_comname[sclass->idnumber]);
//    }
//
//	bool Read(Bit8u * data,Bit16u * size)  {
//        // DTR + RTS on
//        sclass->Write_MCR(0x03);
//        for (Bit16u i=0; i<*size; i++)
//        {
//            Bit8u status;
//            if(!(sclass->Getchar(&data[i],&status,true,1000))) {
//                *size=i;
//                return true;
//            }
//        }
//        return true;
//    }
//	bool Write(Bit8u * data,Bit16u * size) {
//        // DTR + RTS on
//        sclass->Write_MCR(0x03);
//        for (Bit16u i=0; i<*size; i++)
//        {
//            if(!(sclass->Putchar(data[i],true,true,1000))) {
//                *size=i;
//                sclass->Write_MCR(0x01);
//                return false;
//            }
//        }
//        // RTS off
//        sclass->Write_MCR(0x01);
//        return true;
//    }
//	bool Seek(Bit32u * pos,Bit32u type) {
//        *pos = 0;
//	    return true;
//    }
//	bool Close() {
//        return false;
//    }
//	Bit16u GetInformation(void) {
//        return 0x80A0;
//    }
//private:
//	CSerial* sclass;
}
