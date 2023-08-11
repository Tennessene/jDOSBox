package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.*;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;

public class IO extends Module_base {
    static private class IOF_Entry {
        /*Bitu*/int cs;
        /*Bitu*/long eip;
    }
    private final static int IOF_QUEUESIZE = 16;
    static private class IOF_Queue {
        public IOF_Queue() {
            for (int i=0;i<entries.length;i++)
                entries[i] = new IOF_Entry();
        }
        /*Bitu*/int used;
        IOF_Entry[] entries = new IOF_Entry[IOF_QUEUESIZE];
    }
    private static IOF_Queue iof_queue;

    private static CPU.CPU_Decoder IOFaultCore = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
            CPU.CPU_Cycles=1;
            /*Bits*/int ret= Core_full.CPU_Core_Full_Run.call();
            CPU.CPU_CycleLeft+=CPU.CPU_Cycles;
            if (ret<0) Log.exit("Got a dosbox close machine in IO-fault core?");
            if (ret!=0)
                return ret;
            if (iof_queue.used==0) Log.exit("IO-faul Core without IO-faul");
            IOF_Entry entry=iof_queue.entries[iof_queue.used-1];
            if (entry.cs == CPU_Regs.reg_csVal.dword && entry.eip==CPU_Regs.reg_eip)
                return -1;
            return 0;
        }
    };


    /* Some code to make io operations take some virtual time. Helps certain
     * games with their timing of certain operations
     */


    private static final float IODELAY_READ_MICROS = 1.0f;
    private static final float IODELAY_WRITE_MICROS = 0.75f;

    static private void IO_USEC_read_delay_old() {
        if(CPU.CPU_CycleMax > (int)((IODELAY_READ_MICROS*1000.0))) {
            // this could be calculated whenever CPU_CycleMax changes
            /*Bits*/int delaycyc = (int)((CPU.CPU_CycleMax/1000)*IODELAY_READ_MICROS);
            if (CPU.CPU_Cycles > delaycyc) CPU.CPU_Cycles -= delaycyc;
            else CPU.CPU_Cycles = 0;
        }
    }

    static private void IO_USEC_write_delay_old() {
        if(CPU.CPU_CycleMax > (int)((IODELAY_WRITE_MICROS*1000.0))) {
            // this could be calculated whenever CPU_CycleMax changes
            /*Bits*/int delaycyc = (int)((CPU.CPU_CycleMax/1000)*IODELAY_WRITE_MICROS);
            if (CPU.CPU_Cycles > delaycyc) CPU.CPU_Cycles -= delaycyc;
            else CPU.CPU_Cycles = 0;
        }
    }


    static private final int IODELAY_READ_MICROSk = (int)(1024/1.0);
    static private final int IODELAY_WRITE_MICROSk = (int)(1024/0.75);

    static private void IO_USEC_read_delay() {
        /*Bits*/int delaycyc = CPU.CPU_CycleMax/IODELAY_READ_MICROSk;
        if(CPU.CPU_Cycles < 3*delaycyc) delaycyc = 0; //Else port acces will set cycles to 0. which might trigger problem with games which read 16 bit values
        CPU.CPU_Cycles -= delaycyc;
        CPU.CPU_IODelayRemoved += delaycyc;
    }

    static private void IO_USEC_write_delay() {
        /*Bits*/int delaycyc = CPU.CPU_CycleMax/IODELAY_WRITE_MICROSk;
        if(CPU.CPU_Cycles < 3*delaycyc) delaycyc=0;
        CPU.CPU_Cycles -= delaycyc;
        CPU.CPU_IODelayRemoved += delaycyc;
    }

    /*
    static Bit8u crtc_index = 0;
    const char* const len_type[] = {" 8","16","32"};
    void log_io(Bitu width, bool write, Bitu port, Bitu val) {
        switch(width) {
        case 0:
            val&=0xff;
            break;
        case 1:
            val&=0xffff;
            break;
        }
        if (write) {
            // skip the video cursor position spam
            if (port==0x3d4) {
                if (width==0) crtc_index = (Bit8u)val;
                else if(width==1) crtc_index = (Bit8u)(val>>8);
            }
            if (crtc_index==0xe || crtc_index==0xf) {
                if((width==0 && (port==0x3d4 || port==0x3d5))||(width==1 && port==0x3d4))
                    return;
            }

            switch(port) {
            //case 0x020: // interrupt command
            //case 0x040: // timer 0
            //case 0x042: // timer 2
            //case 0x043: // timer control
            //case 0x061: // speaker control
            case 0x3c8: // VGA palette
            case 0x3c9: // VGA palette
            // case 0x3d4: // VGA crtc
            // case 0x3d5: // VGA crtc
            // case 0x3c4: // VGA seq
            // case 0x3c5: // VGA seq
                break;
            default:
                LOG_MSG("iow%s % 4x % 4x, cs:ip %04x:%04x", len_type[width],
                    port, val, SegValue(cs),reg_eip);
                break;
            }
        } else {
            switch(port) {
            //case 0x021: // interrupt status
            //case 0x040: // timer 0
            //case 0x042: // timer 2
            //case 0x061: // speaker control
            case 0x201: // joystick status
            case 0x3c9: // VGA palette
            // case 0x3d4: // VGA crtc index
            // case 0x3d5: // VGA crtc
            case 0x3da: // display status - a real spammer
                // don't log for the above cases
                break;
            default:
                LOG_MSG("ior%s % 4x % 4x,\t\tcs:ip %04x:%04x", len_type[width],
                    port, val, SegValue(cs),reg_eip);
                break;
            }
        }
    }
    #else
    */
    private static void log_io(/*Bitu*/int width, boolean write, /*Bitu*/int port, /*Bitu*/long val) {
//        if (port == 0x3c9 || port == 0x3d4 || port == 0x3d5)
//            return;
//        if (write) {
//            System.out.println("write 0x"+Integer.toHexString(port)+" "+IoHandler.io_writehandlers[width][port]+" val="+val);
//        } else {
//            System.out.println("read 0x"+Integer.toHexString(port)+" "+IoHandler.io_readhandlers[width][port]+" val="+val);
//        }
    }

    public static void IO_WriteB(/*Bitu*/int port,/*Bitu*/int val) {
        log_io(0, true, port, val);
        if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && CPU.CPU_IO_Exception(port,1)) {
            LazyFlags old_lflags = new LazyFlags();
            CPU.CPU_Decoder old_cpudecoder;
            old_cpudecoder=CPU.cpudecoder;
            CPU.cpudecoder=IOFaultCore;
            IOF_Entry entry=iof_queue.entries[iof_queue.used++];
            entry.cs=(int)CPU_Regs.reg_csVal.dword;
            entry.eip=CPU_Regs.reg_eip;
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
            CPU.CPU_Push16(CPU_Regs.reg_ip());
            /*Bit8u*/int old_al = CPU_Regs.reg_eax.low();
            /*Bit16u*/int old_dx = CPU_Regs.reg_edx.word();
            CPU_Regs.reg_eax.low(val);
            CPU_Regs.reg_edx.word(port);
            /*RealPt*/int icb = Callback.CALLBACK_RealPointer(Callback.call_priv_io);
            CPU_Regs.SegSet16CS(Memory.RealSeg(icb));
            CPU_Regs.reg_eip=Memory.RealOff(icb)+0x08;
            CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);

            Dosbox.DOSBOX_RunMachine();
            iof_queue.used--;

            CPU_Regs.reg_eax.low(old_al);
            CPU_Regs.reg_edx.word(old_dx);
            Flags.copy(old_lflags);
            CPU.cpudecoder=old_cpudecoder;
        }
        else {
            IO_USEC_write_delay();
            IoHandler.io_writehandlers[0][port].call(port,val,1);
        }
    }

    static public void IO_WriteW(/*Bitu*/int port,/*Bitu*/int val) {
        log_io(1, true, port, val);
        if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && CPU.CPU_IO_Exception(port,2)) {
            LazyFlags old_lflags = new LazyFlags();
            CPU.CPU_Decoder old_cpudecoder = CPU.cpudecoder;
            CPU.cpudecoder=IOFaultCore;
            IOF_Entry entry=iof_queue.entries[iof_queue.used++];
            entry.cs=CPU_Regs.reg_csVal.dword;
            entry.eip=CPU_Regs.reg_eip;
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
            CPU.CPU_Push16(CPU_Regs.reg_ip());
            /*Bit16u*/int old_ax = CPU_Regs.reg_eax.word();
            /*Bit16u*/int old_dx = CPU_Regs.reg_edx.word();
            CPU_Regs.reg_eax.word(val);
            CPU_Regs.reg_edx.word(port);
            /*RealPt*/int icb = Callback.CALLBACK_RealPointer(Callback.call_priv_io);
            CPU_Regs.SegSet16CS(Memory.RealSeg(icb));
            CPU_Regs.reg_eip=Memory.RealOff(icb)+0x0a;
            CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);

            Dosbox.DOSBOX_RunMachine();
            iof_queue.used--;

            CPU_Regs.reg_eax.word(old_ax);
            CPU_Regs.reg_edx.word(old_dx);
            Flags.copy(old_lflags);
            CPU.cpudecoder=old_cpudecoder;
        }
        else {
            IO_USEC_write_delay();
            IoHandler.io_writehandlers[1][port].call(port,val,2);
        }
    }

    static public void IO_WriteD(/*Bitu*/int port,/*Bitu*/int val) {
        log_io(2, true, port, val);
        if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && CPU.CPU_IO_Exception(port,4)) {
            LazyFlags old_lflags = new LazyFlags();
            CPU.CPU_Decoder old_cpudecoder;
            old_cpudecoder=CPU.cpudecoder;
            CPU.cpudecoder=IOFaultCore;
            IOF_Entry entry=iof_queue.entries[iof_queue.used++];
            entry.cs=CPU_Regs.reg_csVal.dword;
            entry.eip=CPU_Regs.reg_eip;
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
            CPU.CPU_Push16(CPU_Regs.reg_ip());
            /*Bit32u*/int old_eax = CPU_Regs.reg_eax.dword;
            /*Bit16u*/int old_dx = CPU_Regs.reg_edx.word();
            CPU_Regs.reg_eax.dword=val;
            CPU_Regs.reg_edx.word(port);
            /*RealPt*/int icb = Callback.CALLBACK_RealPointer(Callback.call_priv_io);
            CPU_Regs.SegSet16CS(Memory.RealSeg(icb));
            CPU_Regs.reg_eip=Memory.RealOff(icb)+0x0c;
            CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);

            Dosbox.DOSBOX_RunMachine();
            iof_queue.used--;

            CPU_Regs.reg_eax.dword=old_eax;
            CPU_Regs.reg_edx.word(old_dx);
            Flags.copy(old_lflags);
            CPU.cpudecoder=old_cpudecoder;
        } else {
            IoHandler.io_writehandlers[2][port].call(port,val,4);
        }
    }

    static public/*Bitu*/int IO_ReadB(/*Bitu*/int port) {
        /*Bitu*/int retval;
        if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && CPU.CPU_IO_Exception(port,1)) {
            LazyFlags old_lflags = new LazyFlags();
            CPU.CPU_Decoder  old_cpudecoder;
            old_cpudecoder=CPU.cpudecoder;
            CPU.cpudecoder=IOFaultCore;
            IOF_Entry entry=iof_queue.entries[iof_queue.used++];
            entry.cs=CPU_Regs.reg_csVal.dword;
            entry.eip=CPU_Regs.reg_eip;
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
            CPU.CPU_Push16(CPU_Regs.reg_ip());
            /*Bit16u*/int old_dx = CPU_Regs.reg_edx.word();
            CPU_Regs.reg_edx.word(port);
            /*RealPt*/int icb = Callback.CALLBACK_RealPointer(Callback.call_priv_io);
            CPU_Regs.SegSet16CS(Memory.RealSeg(icb));
            CPU_Regs.reg_eip=Memory.RealOff(icb)+0x00;
            CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);

            Dosbox.DOSBOX_RunMachine();
            iof_queue.used--;

            retval = CPU_Regs.reg_eax.low();
            CPU_Regs.reg_edx.word(old_dx);
            Flags.copy(old_lflags);
            CPU.cpudecoder=old_cpudecoder;
            return retval;
        }
        else {
            IO_USEC_read_delay();
            retval = IoHandler.io_readhandlers[0][port].call(port,1);
        }
        log_io(0, false, port, retval);
        return retval;
    }

    static public /*Bitu*/int IO_ReadW(/*Bitu*/int port) {
        /*Bitu*/int retval;
        if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && CPU.CPU_IO_Exception(port,2)) {
            LazyFlags old_lflags = new LazyFlags();
            CPU.CPU_Decoder old_cpudecoder;
            old_cpudecoder=CPU.cpudecoder;
            CPU.cpudecoder=IOFaultCore;
            IOF_Entry entry=iof_queue.entries[iof_queue.used++];
            entry.cs=CPU_Regs.reg_csVal.dword;
            entry.eip=CPU_Regs.reg_eip;
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
            CPU.CPU_Push16(CPU_Regs.reg_ip());
            /*Bit16u*/int old_dx = CPU_Regs.reg_edx.word();
            CPU_Regs.reg_edx.word(port);
            /*RealPt*/int icb = Callback.CALLBACK_RealPointer(Callback.call_priv_io);
            CPU_Regs.SegSet16CS(Memory.RealSeg(icb));
            CPU_Regs.reg_eip=Memory.RealOff(icb)+0x02;
            CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);

            Dosbox.DOSBOX_RunMachine();
            iof_queue.used--;

            retval = CPU_Regs.reg_eax.word();
            CPU_Regs.reg_edx.word(old_dx);
            Flags.copy(old_lflags);
            CPU.cpudecoder=old_cpudecoder;
        }
        else {
            IO_USEC_read_delay();
            retval = IoHandler.io_readhandlers[1][port].call(port,2);
        }
        log_io(1, false, port, retval);
        return retval;
    }

    static public /*Bitu*/int IO_ReadD(/*Bitu*/int port) {
        /*Bitu*/int retval;
        if (CPU_Regs.GETFLAG(CPU_Regs.VM)!=0 && CPU.CPU_IO_Exception(port,4)) {
            LazyFlags old_lflags = new LazyFlags();
            CPU.CPU_Decoder old_cpudecoder;
            old_cpudecoder=CPU.cpudecoder;
            CPU.cpudecoder=IOFaultCore;
            IOF_Entry entry=iof_queue.entries[iof_queue.used++];
            entry.cs=CPU_Regs.reg_csVal.dword;
            entry.eip=CPU_Regs.reg_eip;
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
            CPU.CPU_Push16(CPU_Regs.reg_ip());
            /*Bit16u*/int old_dx = CPU_Regs.reg_edx.word();
            CPU_Regs.reg_edx.word(port);
            /*RealPt*/int icb = Callback.CALLBACK_RealPointer(Callback.call_priv_io);
            CPU_Regs.SegSet16CS(Memory.RealSeg(icb));
            CPU_Regs.reg_eip=Memory.RealOff(icb)+0x04;
            CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);

            Dosbox.DOSBOX_RunMachine();
            iof_queue.used--;

            retval = CPU_Regs.reg_eax.dword;
            CPU_Regs.reg_edx.word(old_dx);
            Flags.copy(old_lflags);
            CPU.cpudecoder=old_cpudecoder;
        } else {
            retval = IoHandler.io_readhandlers[2][port].call(port,4);
        }
        log_io(2, false, port, retval);
        return retval;
    }

    public IO(Section configuration) {
        super(configuration);
        iof_queue.used = 0;
        IoHandler.IO_FreeReadHandler(0,IoHandler.IO_MA,IoHandler.IO_MAX);
	    IoHandler.IO_FreeWriteHandler(0,IoHandler.IO_MA,IoHandler.IO_MAX);
    }
    static IO test;
    public static Section.SectionFunction IO_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
            iof_queue = null;
        }
    };
    public static Section.SectionFunction IO_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            iof_queue = new IOF_Queue();
            test = new IO(sec);
            sec.AddDestroyFunction(IO_Destroy);
        }
    };
}
