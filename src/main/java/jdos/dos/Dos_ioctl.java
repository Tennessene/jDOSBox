package jdos.dos;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringHelper;

public class Dos_ioctl {
    static public boolean DOS_IOCTL() {
        /*Bitu*/int handle=0;/*Bit8u*/short drive=0;
        /* calls 0-4,6,7,10,12,16 use a file handle */
        if ((CPU_Regs.reg_eax.low()<4) || (CPU_Regs.reg_eax.low()==0x06) || (CPU_Regs.reg_eax.low()==0x07) || (CPU_Regs.reg_eax.low()==0x0a) || (CPU_Regs.reg_eax.low()==0x0c) || (CPU_Regs.reg_eax.low()==0x10)) {
            handle=Dos.RealHandle(CPU_Regs.reg_ebx.word());
            if (handle>=Dos_files.DOS_FILES) {
                Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
                return false;
            }
            if (Dos_files.Files[handle]==null) {
                Dos.DOS_SetError(Dos.DOSERR_INVALID_HANDLE);
                return false;
            }
        } else if (CPU_Regs.reg_eax.low()<0x12) { 				/* those use a diskdrive except 0x0b */
            if (CPU_Regs.reg_eax.low()!=0x0b) {
                drive=(short)CPU_Regs.reg_ebx.low();if (drive==0) drive = Dos_files.DOS_GetDefaultDrive();else drive--;
                if((drive >= 2) && !(( drive < Dos_files.DOS_DRIVES ) && Dos_files.Drives[drive]!=null) ) {
                    Dos.DOS_SetError(Dos.DOSERR_INVALID_DRIVE);
                    return false;
                }
            }
        } else {
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC, LogSeverities.LOG_ERROR,"DOS:IOCTL Call "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" unhandled");
            Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
            return false;
        }
        switch(CPU_Regs.reg_eax.low()) {
        case 0x00:		/* Get Device Information */
            if ((Dos_files.Files[handle].GetInformation() & 0x8000)!=0) {	//Check for device
                CPU_Regs.reg_edx.word(Dos_files.Files[handle].GetInformation());
            } else {
                /*Bit8u*/short hdrive=Dos_files.Files[handle].GetDrive();
                if (hdrive==0xff) {
                    Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"00:No drive set");
                    hdrive=2;	// defaulting to C:
                }
                /* return drive number in lower 5 bits for block devices */
                CPU_Regs.reg_edx.word((Dos_files.Files[handle].GetInformation()&0xffe0)|hdrive);
            }
            CPU_Regs.reg_eax.word(CPU_Regs.reg_edx.word()); //Destroyed officially
            return true;
        case 0x01:		/* Set Device Information */
            if (CPU_Regs.reg_edx.high() != 0) {
                Dos.DOS_SetError(Dos.DOSERR_DATA_INVALID);
                return false;
            } else {
                if ((Dos_files.Files[handle].GetInformation() & 0x8000)!=0) {	//Check for device
                    CPU_Regs.reg_eax.low((/*Bit8u*/short)(Dos_files.Files[handle].GetInformation() & 0xff));
                } else {
                    Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
            }
            return true;
        case 0x02:		/* Read from Device Control Channel */
            if ((Dos_files.Files[handle].GetInformation() & 0xc000)!=0) {
                /* is character device with IOCTL support */
                /*PhysPt*/int bufptr= Memory.PhysMake((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word());
                /*Bit16u*/IntRef retcode=new IntRef(0);
                if (((DOS_Device)(Dos_files.Files[handle])).ReadFromControlChannel(bufptr,CPU_Regs.reg_ecx.word(),retcode)) {
                    CPU_Regs.reg_eax.word(retcode.value);
                    return true;
                }
            }
            Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
            return false;
        case 0x03:		/* Write to Device Control Channel */
            if ((Dos_files.Files[handle].GetInformation() & 0xc000)!=0) {
                /* is character device with IOCTL support */
                /*PhysPt*/int bufptr=Memory.PhysMake((int)CPU_Regs.reg_dsVal.dword,CPU_Regs.reg_edx.word());
                /*Bit16u*/IntRef retcode=new IntRef(0);
                if (((DOS_Device)(Dos_files.Files[handle])).WriteToControlChannel(bufptr,CPU_Regs.reg_ecx.word(),retcode)) {
                    CPU_Regs.reg_eax.word(retcode.value);
                    return true;
                }
            }
            Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
            return false;
        case 0x06:      /* Get Input Status */
            if ((Dos_files.Files[handle].GetInformation() & 0x8000)!=0) {		//Check for device
                CPU_Regs.reg_eax.low((Dos_files.Files[handle].GetInformation() & 0x40)!=0 ? 0x0 : 0xff);
            } else { // FILE
                /*Bit32u*/LongRef oldlocation=new LongRef(0);
                Dos_files.Files[handle].Seek(oldlocation, Dos_files.DOS_SEEK_CUR);
                /*Bit32u*/LongRef endlocation=new LongRef(0);
                Dos_files.Files[handle].Seek(endlocation, Dos_files.DOS_SEEK_END);
                if(oldlocation.value < endlocation.value){//Still data available
                    CPU_Regs.reg_eax.low(0xff);
                } else {
                    CPU_Regs.reg_eax.low(0x0); //EOF or beyond
                }
                Dos_files.Files[handle].Seek(oldlocation, Dos_files.DOS_SEEK_SET); //restore filelocation
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"06:Used Get Input Status on regular file with handle "+handle);
            }
            return true;
        case 0x07:		/* Get Output Status */
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_NORMAL,"07:Fakes output status is ready for handle "+handle);
            CPU_Regs.reg_eax.low(0xff);
            return true;
        case 0x08:		/* Check if block device removable */
            /* cdrom drives and drive a&b are removable */
            if (drive < 2) CPU_Regs.reg_eax.word(0);
            else if (!Dos_files.Drives[drive].isRemovable()) CPU_Regs.reg_eax.word(1);
            else {
                Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            }
            return true;
        case 0x09:		/* Check if block device remote */
            if ((drive >= 2) && Dos_files.Drives[drive].isRemote()) {
                CPU_Regs.reg_edx.word(0x1000);	// device is remote
                // undocumented bits always clear
            } else {
                CPU_Regs.reg_edx.word(0x0802);	// Open/Close supported; 32bit access supported (any use? fixes Fable installer)
                // undocumented bits from device attribute word
                // TODO Set bit 9 on drives that don't support direct I/O
            }
            CPU_Regs.reg_eax.word(0x300);
            return true;
        case 0x0B:		/* Set sharing retry count */
            if (CPU_Regs.reg_edx.word()==0) {
                Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            }
            return true;
        case 0x0D:		/* Generic block device request */
            {
                if ((drive >= 2) && Dos_files.Drives[drive].isRemovable()) {
                    Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
                /*PhysPt*/int ptr = CPU_Regs.reg_dsPhys.dword+CPU_Regs.reg_edx.word();
                switch (CPU_Regs.reg_ecx.low()) {
                case 0x60:		/* Get Device parameters */
                    Memory.mem_writeb(ptr  ,0x03);					// special function
                    Memory.mem_writeb(ptr+1,(drive>=2)?0x05:0x14);	// fixed disc(5), 1.44 floppy(14)
                    Memory.mem_writew(ptr+2,drive>=2?1:0);				// nonremovable ?
                    Memory.mem_writew(ptr+4,0x0000);				// num of cylinders
                    Memory.mem_writeb(ptr+6,0x00);					// media type (00=other type)
                    // drive parameter block following
                    Memory.mem_writeb(ptr+7,drive);				// drive
                    Memory.mem_writeb(ptr+8,0x00);					// unit number
                    Memory.mem_writed(ptr+0x1f,0xffffffff);		// next parameter block
                    break;
                case 0x46:
                case 0x66:	/* Volume label */
                    {
                        String bufin=Dos_files.Drives[drive].GetLabel();
                        byte[] buffer ={' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' '};

                        int find_ext=bufin.indexOf('.');
                        if (find_ext>=0) {
                            /*Bitu*/int size=find_ext;
                            if (size>8) size=8;
                            StringHelper.strcpy(buffer, bufin.substring(size));
                            find_ext++;
                            String ext = bufin.substring(find_ext+1);
                            if (ext.length()>3)
                                ext = ext.substring(0, 3);
                            StringHelper.strcpy(buffer, size, ext);
                        } else {
                            if (bufin.length()>8)
                                bufin = bufin.substring(0, 8);
                            StringHelper.strcpy(buffer, bufin);
                        }

                        byte[] buf2={ 'F','A','T','1','6',' ',' ',' '};
                        if(drive<2) buf2[4] = '2'; //FAT12 for floppies

                        Memory.mem_writew(ptr+0,0);			// 0
                        Memory.mem_writed(ptr+2,0x1234);		//Serial number
                        Memory.MEM_BlockWrite(ptr+6,new String(buffer),11);//volumename
                        if(CPU_Regs.reg_ecx.low() == 0x66) Memory.MEM_BlockWrite(ptr+0x11, new String(buf2),8);//filesystem
                    }
                    break;
                default	:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_IOCTL,LogSeverities.LOG_ERROR,"DOS:IOCTL Call 0D:"+Integer.toString(CPU_Regs.reg_ecx.low(), 16)+" Drive "+Integer.toString(drive, 16)+" unhandled");
                    Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
                return true;
            }
        case 0x0E:			/* Get Logical Drive Map */
            if (drive < 2) {
                if (Dos_files.Drives[drive]!=null) CPU_Regs.reg_eax.low(drive+1);
                else CPU_Regs.reg_eax.low(1);
            } else if (Dos_files.Drives[drive].isRemovable()) {
                Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            } else CPU_Regs.reg_eax.low(0);	/* Only 1 logical drive assigned */
            CPU_Regs.reg_eax.high(0x07);
            return true;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_DOSMISC,LogSeverities.LOG_ERROR,"DOS:IOCTL Call "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" unhandled");
            Dos.DOS_SetError(Dos.DOSERR_FUNCTION_NUMBER_INVALID);
            break;
        }
        return false;
    }


    static public boolean DOS_GetSTDINStatus() {
        /*Bit32u*/int handle=Dos.RealHandle(Dos_files.STDIN);
        if (handle==0xFF) return false;
        if (Dos_files.Files[handle]!=null && (Dos_files.Files[handle].GetInformation() & 64)!=0) return false;
        return true;
    }
}
