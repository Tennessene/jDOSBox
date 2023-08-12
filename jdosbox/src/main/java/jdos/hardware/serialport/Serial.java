package jdos.hardware.serialport;

import jdos.gui.Main;
import jdos.hardware.Pic;
import jdos.misc.setup.CommandLine;
import jdos.util.FileIO;
import jdos.util.IntRef;
import jdos.util.ShortRef;
import jdos.util.StringHelper;

public class Serial {
    //  DUMMY
    public boolean Getchar(ShortRef data, ShortRef lsr, boolean wait_dsr, /*Bitu*/int timeout) {
        return false;
    }
    public boolean Putchar(short data, boolean wait_dtr, boolean wait_rts, /*Bitu*/int timeout) {
        return false;
    }
    ///////////////////////////

    private static final boolean SERIAL_DEBUG = false;
    // Serial port interface

    static private class MyFifo {
        MyFifo(/*Bitu*/int maxsize_) {
            maxsize=size=maxsize_;
            pos=used=0;
            data=new /*Bit8u*/short[size];
        }
        /*Bitu*/int getFree() {
            return size-used;
        }
        boolean isEmpty() {
            return used==0;
        }
        boolean isFull() {
            return (size-used)==0;
        }

        /*Bitu*/int getUsage() {
            return used;
        }
        void setSize(/*Bitu*/int newsize)
        {
            size=newsize;
            pos=used=0;
        }
        void clear() {
            pos=used=0;
            data[0]=0;
        }

        boolean addb(/*Bit8u*/short _val) {
            /*Bitu*/int where=pos+used;
            if (where>=size) where-=size;
            if(used>=size) {
                // overwrite last byte
                if(where==0) where=size-1;
                else where--;
                data[where]=_val;
                return false;
            }
            data[where]=_val;
            used++;
            return true;
        }
        /*Bit8u*/short getb() {
            if (used==0) return data[pos];
            /*Bitu*/int where=pos;
            used--;
            if(used!=0) pos++;
            if (pos>=size) pos-=size;
            return data[where];
        }
        /*Bit8u*/short getTop() {
            /*Bitu*/int where=pos+used;
            if (where>=size) where-=size;
            if(used>=size) {
                if(where==0) where=size-1;
                else where--;
            }
            return data[where];
        }

        /*Bit8u*/short probeByte() {
            return data[pos];
        }
        /*Bit8u*/short[] data;
        /*Bitu*/int maxsize,size,pos,used;
    }

	FileIO debugfp;
	boolean dbg_modemcontrol; // RTS,CTS,DTR,DSR,RI,CD
	boolean dbg_serialtraffic;
	boolean dbg_register;
	boolean dbg_interrupt;
	boolean dbg_aux;
	void log_ser(boolean active, String format) {
        if(active) {
            String buf = StringHelper.format(Pic.PIC_FullIndex(), 3)+" ["+StringHelper.format(Main.GetTicks(), 7)+"] ";
            buf+=format;
            if(!buf.endsWith("\n")) buf+="\r\n";
            try {debugfp.write(buf.getBytes());} catch (Exception e){}
        }
    }

	static boolean getBituSubstring(String name,/*Bitu*/IntRef data, CommandLine cmd) {
        String tmpstring;
        if((tmpstring=cmd.FindStringBegin(name,false))==null) return false;
        try {
            data.value = Integer.parseInt(tmpstring);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

	boolean InstallationSuccessful;// check after constructing. If
								// something was wrong, delete it right away.

//	// Constructor takes com port number (0-3)
//	Serial(/*Bitu*/int id, CommandLine cmd) {
//        idnumber=id;
//        Bit16u base = serial_baseaddr[id];
//
//        irq = serial_defaultirq[id];
//        get/*Bitu*/intSubstring("irq:",&irq, cmd);
//        if (irq < 2 || irq > 15) irq = serial_defaultirq[id];
//
//    #if SERIAL_DEBUG
//        dbg_serialtraffic = cmd->FindExist("dbgtr", false);
//        dbg_modemcontrol  = cmd->FindExist("dbgmd", false);
//        dbg_register      = cmd->FindExist("dbgreg", false);
//        dbg_interrupt     = cmd->FindExist("dbgirq", false);
//        dbg_aux			  = cmd->FindExist("dbgaux", false);
//
//        if(cmd->FindExist("dbgall", false)) {
//            dbg_serialtraffic=
//            dbg_modemcontrol=
//            dbg_register=
//            dbg_interrupt=
//            dbg_aux= true;
//        }
//
//
//        if(dbg_serialtraffic|dbg_modemcontrol|dbg_register|dbg_interrupt|dbg_aux)
//            debugfp=OpenCaptureFile("serlog",".serlog.txt");
//        else debugfp=0;
//
//        if(debugfp == 0) {
//            dbg_serialtraffic=
//            dbg_modemcontrol=
//            dbg_register=
//            dbg_interrupt=
//            dbg_aux= false;
//        } else {
//            std::string cleft;
//            cmd->GetStringRemain(cleft);
//
//            log_ser(true,"Serial%d: BASE %3x, IRQ %d, initstring \"%s\"\r\n\r\n",
//                COMNUMBER,base,irq,cleft.c_str());
//        }
//    #endif
//        fifosize=16;
//
//        errorfifo = new MyFifo(fifosize);
//        rxfifo = new MyFifo(fifosize);
//        txfifo = new MyFifo(fifosize);
//
//        mydosdevice=new device_COM(this);
//        DOS_AddDevice(mydosdevice);
//
//        errormsg_pending=false;
//        framingErrors=0;
//        parityErrors=0;
//        overrunErrors=0;
//        txOverrunErrors=0;
//        overrunIF0=0;
//        breakErrors=0;
//
//        for (/*Bitu*/int i = 0; i <= 7; i++) {
//            WriteHandler[i].Install (i + base, SERIAL_Write, IO_MB);
//            ReadHandler[i].Install (i + base, SERIAL_Read, IO_MB);
//        }
//    }
//
//	virtual ~CSerial() {
//        DOS_DelDevice(mydosdevice);
//        for(/*Bitu*/int i = 0; i <= SERIAL_BASE_EVENT_COUNT; i++)
//            removeEvent(i);
//    }
//
//	IO_ReadHandleObject ReadHandler[8];
//	IO_WriteHandleObject WriteHandler[8];
//
//	float bytetime; // how long a byte takes to transmit/receive in milliseconds
//	void changeLineProperties() {
//            // update the event wait time
//        float bitlen;
//
//        if(baud_divider==0) bitlen=(1000.0f/115200.0f);
//        else bitlen = (1000.0f/115200.0f)*(float)baud_divider;
//        bytetime=bitlen*(float)(1+5+1);		// startbit + minimum length + stopbit
//        bytetime+= bitlen*(float)(LCR&0x3); // databits
//        if(LCR&0x4) bytetime+=bitlen;		// stopbit
//
//    #if SERIAL_DEBUG
//        const char* const dbgtext[]={"none","odd","none","even","none","mark","none","space"};
//        log_ser(dbg_serialtraffic,"New COM parameters: baudrate %5.0f, parity %s, wordlen %d, stopbits %d",
//            1.0/bitlen*1000.0f,dbgtext[(LCR&0x38)>>3],(LCR&0x3)+5,((LCR&0x4)>>2)+1);
//    #endif
//        updatePortConfig (baud_divider, LCR);
//    }
//	/*Bitu*/int idnumber;
//
//	void setEvent(Bit16u type, float duration) {
//        PIC_AddEvent(Serial_EventHandler,duration,(type<<2)|idnumber);
//    }
//	void removeEvent(Bit16u type) {
//        // TODO
//	    PIC_RemoveSpecificEvents(Serial_EventHandler,(type<<2)|idnumber);
//    }
//	void handleEvent(Bit16u type) {
//        switch(type) {
//            case SERIAL_TX_LOOPBACK_EVENT: {
//
//    #if SERIAL_DEBUG
//                log_ser(dbg_serialtraffic,loopback_data<0x10?
//                    "tx 0x%02x (%u) (loopback)":"tx 0x%02x (%c) (loopback)",
//                    loopback_data, loopback_data);
//    #endif
//                receiveByte (loopback_data);
//                ByteTransmitted ();
//                break;
//            }
//            case SERIAL_THR_LOOPBACK_EVENT: {
//                loopback_data=txfifo->probeByte();
//                ByteTransmitting();
//                setEvent(SERIAL_TX_LOOPBACK_EVENT,bytetime);
//                break;
//            }
//            case SERIAL_ERRMSG_EVENT: {
//                LOG_MSG("Serial%d: Errors: "\
//                    "Framing %d, Parity %d, Overrun RX:%d (IF0:%d), TX:%d, Break %d",
//                    COMNUMBER, framingErrors, parityErrors, overrunErrors,
//                    overrunIF0,txOverrunErrors, breakErrors);
//                errormsg_pending=false;
//                framingErrors=0;
//                parityErrors=0;
//                overrunErrors=0;
//                txOverrunErrors=0;
//                overrunIF0=0;
//                breakErrors=0;
//                break;
//            }
//            case SERIAL_RX_TIMEOUT_EVENT: {
//                rise(TIMEOUT_PRIORITY);
//                break;
//            }
//            default: handleUpperEvent(type);
//        }
//    }
//	virtual void handleUpperEvent(Bit16u type)=0;
//
//	// defines for event type
//#define SERIAL_TX_LOOPBACK_EVENT 0
//#define SERIAL_THR_LOOPBACK_EVENT 1
//#define SERIAL_ERRMSG_EVENT 2
//
//#define SERIAL_TX_EVENT 3
//#define SERIAL_RX_EVENT 4
//#define SERIAL_POLLING_EVENT 5
//#define SERIAL_THR_EVENT 6
//#define SERIAL_RX_TIMEOUT_EVENT 7
//
//#define	SERIAL_BASE_EVENT_COUNT 7
//
//#define COMNUMBER idnumber+1
//
//	/*Bitu*/int irq;
//
//	// CSerial requests an update of the input lines
//	virtual void updateMSR()=0;
//
//	// Control lines from prepherial to serial port
//	boolean getDTR() {
//        if(loopback) return false;
//	    else return dtr;
//    }
//	boolean getRTS() {
//        if(loopback) return false;
//	    else return rts;
//    }
//
//	boolean getRI() {
//        return ri;
//    }
//	boolean getCD() {
//        return cd;
//    }
//	boolean getDSR() {
//        return dsr;
//    }
//	boolean getCTS() {
//        return cts;
//    }
//
//	void setRI(boolean value) {
//        if (value != ri) {
//
//    #if SERIAL_DEBUG
//            log_ser(dbg_modemcontrol,"%RI  %x.",value);
//    #endif
//            // don't change delta when in loopback mode
//            ri=value;
//            if(!loopback) {
//                if(value==false) d_ri=true;
//                rise (MSR_PRIORITY);
//            }
//        }
//        //else no change
//    }
//	void setDSR(boolean value) {
//        if (value != dsr) {
//    #if SERIAL_DEBUG
//            log_ser(dbg_modemcontrol,"DSR %x.",value);
//    #endif
//            // don't change delta when in loopback mode
//            dsr=value;
//            if(!loopback) {
//                d_dsr=true;
//                rise (MSR_PRIORITY);
//            }
//        }
//        //else no change
//    }
//    void setCD(boolean value) {
//        if (value != cd) {
//    #if SERIAL_DEBUG
//            log_ser(dbg_modemcontrol,"CD  %x.",value);
//    #endif
//            // don't change delta when in loopback mode
//            cd=value;
//            if(!loopback) {
//                d_cd=true;
//                rise (MSR_PRIORITY);
//            }
//        }
//        //else no change
//    }
//
//	void setCTS(boolean value); {
//        if (value != cts) {
//    #if SERIAL_DEBUG
//            log_ser(dbg_modemcontrol,"CTS %x.",value);
//    #endif
//            // don't change delta when in loopback mode
//            cts=value;
//            if(!loopback) {
//                d_cts=true;
//                rise (MSR_PRIORITY);
//            }
//        }
//        //else no change
//    }
//
//	// From serial port to prepherial
//	// set output lines
//	virtual void setRTSDTR(boolean rts, boolean dtr)=0;
//	virtual void setRTS(boolean val)=0;
//	virtual void setDTR(boolean val)=0;
//
//	// Register access
//	void Write_THR(Bit8u data) {
//        // 0-7 transmit data
//
//        if ((LCR & LCR_DIVISOR_Enable_MASK)) {
//            // write to DLL
//            baud_divider&=0xFF00;
//            baud_divider |= data;
//            changeLineProperties();
//        } else {
//            // write to THR
//            clear (TX_PRIORITY);
//
//            if((LSR & LSR_TX_EMPTY_MASK))
//            {	// we were idle before
//                //LOG_MSG("starting new transmit cycle");
//                //if(sync_guardtime) LOG_MSG("Serial port internal error 1");
//                //if(!(LSR & LSR_TX_EMPTY_MASK)) LOG_MSG("Serial port internal error 2");
//                //if(txfifo->getUsage()) LOG_MSG("Serial port internal error 3");
//
//                // need "warming up" time
//                sync_guardtime=true;
//                // block the fifo so it returns THR full (or not in case of FIFO on)
//                txfifo->addb(data);
//                // transmit shift register is busy
//                LSR &= (~LSR_TX_EMPTY_MASK);
//                if(loopback) setEvent(SERIAL_THR_LOOPBACK_EVENT, bytetime/10);
//                else {
//    #if SERIAL_DEBUG
//                    log_ser(dbg_serialtraffic,data<0x10?
//                        "\t\t\t\t\ttx 0x%02x (%u) [FIFO=%2d]":
//                        "\t\t\t\t\ttx 0x%02x (%c) [FIFO=%2d]",data,data,txfifo->getUsage());
//    #endif
//                    transmitByte (data,true);
//                }
//            } else {
//                //  shift register is transmitting
//                if(!txfifo->addb(data)) {
//                    // TX overflow
//    #if SERIAL_DEBUG
//                    log_ser(dbg_serialtraffic,"tx overflow");
//    #endif
//                    txOverrunErrors++;
//                    if(!errormsg_pending) {
//                        errormsg_pending=true;
//                        setEvent(SERIAL_ERRMSG_EVENT,1000);
//                    }
//                }
//            }
//        }
//    }
//	void Write_IER(Bit8u data) {
//        if ((LCR & LCR_DIVISOR_Enable_MASK)) {	// write to DLM
//            baud_divider&=0xff;
//            baud_divider |= ((Bit16u)data)<<8;
//            changeLineProperties();
//        } else {
//            // Retrigger TX interrupt
//            if (txfifo->isEmpty()&& (data&TX_PRIORITY))
//                waiting_interrupts |= TX_PRIORITY;
//
//            IER = data&0xF;
//            if((FCR&FCR_ACTIVATE)&&data&RX_PRIORITY) IER |= TIMEOUT_PRIORITY;
//            ComputeInterrupts();
//        }
//    }
//    #define BIT_CHANGE_H(oldv,newv,bitmask) (!(oldv&bitmask) && (newv&bitmask))
//    #define BIT_CHANGE_L(oldv,newv,bitmask) ((oldv&bitmask) && !(newv&bitmask))
//
//	void Write_FCR(Bit8u data) {
//        if(BIT_CHANGE_H(FCR,data,FCR_ACTIVATE)) {
//            // FIFO was switched on
//            errors_in_fifo=0; // should already be 0
//            errorfifo->setSize(fifosize);
//            rxfifo->setSize(fifosize);
//            txfifo->setSize(fifosize);
//        } else if(BIT_CHANGE_L(FCR,data,FCR_ACTIVATE)) {
//            // FIFO was switched off
//            errors_in_fifo=0;
//            errorfifo->setSize(1);
//            rxfifo->setSize(1);
//            txfifo->setSize(1);
//            rx_interrupt_threshold=1;
//        }
//        FCR=data&0xCF;
//        if(FCR&FCR_CLEAR_RX) {
//            errors_in_fifo=0;
//            errorfifo->clear();
//            rxfifo->clear();
//        }
//        if(FCR&FCR_CLEAR_TX) txfifo->clear();
//        if(FCR&FCR_ACTIVATE) {
//            switch(FCR>>6) {
//                case 0: rx_interrupt_threshold=1; break;
//                case 1: rx_interrupt_threshold=4; break;
//                case 2: rx_interrupt_threshold=8; break;
//                case 3: rx_interrupt_threshold=14; break;
//            }
//        }
//    }
//	void Write_LCR(Bit8u data) {
//        Bit8u lcr_old = LCR;
//        LCR = data;
//        if (((data ^ lcr_old) & LCR_PORTCONFIG_MASK) != 0) {
//            changeLineProperties();
//        }
//        if (((data ^ lcr_old) & LCR_BREAK_MASK) != 0) {
//            if(!loopback) setBreak ((LCR & LCR_BREAK_MASK)!=0);
//            else {
//                // TODO: set loopback break event to reveiveError after
//            }
//    #if SERIAL_DEBUG
//            log_ser(dbg_serialtraffic,((LCR & LCR_BREAK_MASK)!=0) ?
//                "break on.":"break off.");
//    #endif
//        }
//    }
//	void Write_MCR(Bit8u data) {
//        // WARNING: At the time setRTSDTR is called rts and dsr members are still wrong.
//        if(data&FIFO_FLOWCONTROL) LOG_MSG("Warning: tried to activate hardware handshake.");
//        boolean temp_dtr = data & MCR_DTR_MASK? true:false;
//        boolean temp_rts = data & MCR_RTS_MASK? true:false;
//        boolean temp_op1 = data & MCR_OP1_MASK? true:false;
//        boolean temp_op2 = data & MCR_OP2_MASK? true:false;
//        boolean temp_loopback = data & MCR_LOOPBACK_Enable_MASK? true:false;
//        if(loopback!=temp_loopback) {
//            if(temp_loopback) setRTSDTR(false,false);
//            else setRTSDTR(temp_rts,temp_dtr);
//        }
//
//        if (temp_loopback) {	// is on:
//            // DTR->DSR
//            // RTS->CTS
//            // OP1->RI
//            // OP2->CD
//            if(temp_dtr!=dtr && !d_dsr) {
//                d_dsr=true;
//                rise (MSR_PRIORITY);
//            }
//            if(temp_rts!=rts && !d_cts) {
//                d_cts=true;
//                rise (MSR_PRIORITY);
//            }
//            if(temp_op1!=op1 && !d_ri) {
//                // interrupt only at trailing edge
//                if(!temp_op1) {
//                    d_ri=true;
//                    rise (MSR_PRIORITY);
//                }
//            }
//            if(temp_op2!=op2 && !d_cd) {
//                d_cd=true;
//                rise (MSR_PRIORITY);
//            }
//        } else {
//            // loopback is off
//            if(temp_rts!=rts) {
//                // RTS difference
//                if(temp_dtr!=dtr) {
//                    // both difference
//
//    #if SERIAL_DEBUG
//                    log_ser(dbg_modemcontrol,"RTS %x.",temp_rts);
//                    log_ser(dbg_modemcontrol,"DTR %x.",temp_dtr);
//    #endif
//                    setRTSDTR(temp_rts, temp_dtr);
//                } else {
//                    // only RTS
//
//    #if SERIAL_DEBUG
//                    log_ser(dbg_modemcontrol,"RTS %x.",temp_rts);
//    #endif
//                    setRTS(temp_rts);
//                }
//            } else if(temp_dtr!=dtr) {
//                // only DTR
//    #if SERIAL_DEBUG
//                    log_ser(dbg_modemcontrol,"%DTR %x.",temp_dtr);
//    #endif
//                setDTR(temp_dtr);
//            }
//        }
//        // interrupt logic: if OP2 is 0, the IRQ line is tristated (pulled high)
//        if((!op2) && temp_op2) {
//            // irq has been enabled (tristate high -> irq level)
//            if(!irq_active) PIC_DeActivateIRQ(irq);
//        } else if(op2 && (!temp_op2)) {
//            if(!irq_active) PIC_ActivateIRQ(irq);
//        }
//
//        dtr=temp_dtr;
//        rts=temp_rts;
//        op1=temp_op1;
//        op2=temp_op2;
//        loopback=temp_loopback;
//    }
//	// Really old hardware seems to have the delta part of this register writable
//	void Write_MSR(Bit8u data) {
//        d_cts = (val&MSR_dCTS_MASK)?true:false;
//        d_dsr = (val&MSR_dDSR_MASK)?true:false;
//        d_cd = (val&MSR_dCD_MASK)?true:false;
//        d_ri = (val&MSR_dRI_MASK)?true:false;
//    }
//	void Write_SPR(Bit8u data) {
//        SPR = data;
//    }
//
//	void Write_reserved(Bit8u data, Bit8u address) {
//        /*LOG_UART("Serial%d: Write to reserved register, value 0x%x, register %x",
//		COMNUMBER, data, address);*/
//    }
//
//	/*Bitu*/int Read_RHR() {
//        // 0-7 received data
//        if ((LCR & LCR_DIVISOR_Enable_MASK)) return baud_divider&0xff;
//        else {
//            Bit8u data=rxfifo->getb();
//            if(FCR&FCR_ACTIVATE) {
//                Bit8u error=errorfifo->getb();
//                if(error) errors_in_fifo--;
//                // new error
//                if(!rxfifo->isEmpty()) {
//                    error=errorfifo->probeByte();
//                    if(error) {
//                        LSR |= error;
//                        rise(ERROR_PRIORITY);
//                    }
//                }
//            }
//            // Reading RHR resets the FIFO timeout
//            clear (TIMEOUT_PRIORITY);
//            // RX int. is cleared if the buffer holds less data than the threshold
//            if(rxfifo->getUsage()<rx_interrupt_threshold)clear(RX_PRIORITY);
//            removeEvent(SERIAL_RX_TIMEOUT_EVENT);
//            if(!rxfifo->isEmpty()) setEvent(SERIAL_RX_TIMEOUT_EVENT,bytetime*4.0f);
//            return data;
//        }
//    }
//	/*Bitu*/int Read_IER() {
//        // 0	receive holding register (byte received)
//        // 1	transmit holding register (byte sent)
//        // 2	receive line status (overrun, parity error, frame error, break)
//        // 3	modem status
//        // 4-7	0
//
//        if (LCR & LCR_DIVISOR_Enable_MASK) return baud_divider>>8;
//        else return IER&0x0f;
//    }
//	/*Bitu*/int Read_ISR() {
//        // 0	0:interrupt pending 1: no interrupt
//        // 1-3	identification
//        //      011 LSR
//        //		010 RXRDY
//        //		110 RX_TIMEOUT
//        //		001 TXRDY
//        //		000 MSR
//        // 4-7	0
//
//        if(IER&Modem_Status_INT_Enable_MASK) updateMSR();
//        Bit8u retval = ISR;
//
//        // clear changes ISR!! mean..
//        if(ISR==ISR_TX_VAL) clear(TX_PRIORITY);
//        if(FCR&FCR_ACTIVATE) retval |= FIFO_STATUS_ACTIVE;
//
//        return retval;
//    }
//    /*****************************************************************************/
//    /* Line Control Register (r/w)                                              **/
//    /*****************************************************************************/
//    // signal decoder configuration:
//    // - parity, stopbits, word length
//    // - send break
//    // - switch between RHR/THR and baud rate registers
//    // Modified by:
//    // - writing to it.
//	/*Bitu*/int Read_LCR() {
//        // 0-1	word length
//        // 2	stop bits
//        // 3	parity enable
//        // 4-5	parity type
//        // 6	set break
//        // 7	divisor latch enable
//        return LCR;
//    }
//    /*****************************************************************************/
//    /* Modem Control Register (r/w)                                             **/
//    /*****************************************************************************/
//    // Set levels of RTS and DTR, as well as loopback-mode.
//    // Modified by:
//    // - writing to it.
//	/*Bitu*/int Read_MCR() {
//        // 0	-DTR
//        // 1	-RTS
//        // 2	-OP1
//        // 3	-OP2
//        // 4	loopback enable
//        // 5-7	0
//        Bit8u retval=0;
//        if(dtr) retval|=MCR_DTR_MASK;
//        if(rts) retval|=MCR_RTS_MASK;
//        if(op1) retval|=MCR_OP1_MASK;
//        if(op2) retval|=MCR_OP2_MASK;
//        if(loopback) retval|=MCR_LOOPBACK_Enable_MASK;
//        return retval;
//    }
//    /*****************************************************************************/
//    /* Line Status Register (r)                                                 **/
//    /*****************************************************************************/
//    // errors, tx registers status, rx register status
//    // modified by:
//    // - event from real serial port
//    // - loopback
//	/*Bitu*/int Read_LSR() {
//        /*Bitu*/int retval = LSR & (LSR_ERROR_MASK|LSR_TX_EMPTY_MASK);
//        if(txfifo->isEmpty()) retval |= LSR_TX_HOLDING_EMPTY_MASK;
//        if(!(rxfifo->isEmpty()))retval |= LSR_RX_DATA_READY_MASK;
//        if(errors_in_fifo) retval |= FIFO_ERROR;
//        LSR &= (~LSR_ERROR_MASK);			// clear error bits on read
//        clear (ERROR_PRIORITY);
//        return retval;
//    }
//    /*****************************************************************************/
//    /* Modem Status Register (r)                                                **/
//    /*****************************************************************************/
//    // Contains status of the control input lines (CD, RI, DSR, CTS) and
//    // their "deltas": if level changed since last read delta = 1.
//    // modified by:
//    // - real values
//    // - write operation to MCR in loopback mode
//	/*Bitu*/int Read_MSR() {
//        Bit8u retval=0;
//
//        if (loopback) {
//
//            if (rts) retval |= MSR_CTS_MASK;
//            if (dtr) retval |= MSR_DSR_MASK;
//            if (op1) retval |= MSR_RI_MASK;
//            if (op2) retval |= MSR_CD_MASK;
//
//        } else {
//
//            updateMSR();
//            if (cd) retval |= MSR_CD_MASK;
//            if (ri) retval |= MSR_RI_MASK;
//            if (dsr) retval |= MSR_DSR_MASK;
//            if (cts) retval |= MSR_CTS_MASK;
//
//        }
//        // new delta flags
//        if(d_cd) retval|=MSR_dCD_MASK;
//        if(d_ri) retval|=MSR_dRI_MASK;
//        if(d_cts) retval|=MSR_dCTS_MASK;
//        if(d_dsr) retval|=MSR_dDSR_MASK;
//
//        d_cd = false;
//        d_ri = false;
//        d_cts = false;
//        d_dsr = false;
//
//        clear (MSR_PRIORITY);
//        return retval;
//    }
//    /*****************************************************************************/
//    /* Scratchpad Register (r/w)                                                **/
//    /*****************************************************************************/
//    // Just a memory register. Not much to do here.
//	/*Bitu*/int Read_SPR() {
//        return SPR;
//    }
//
//	// If a byte comes from loopback or prepherial, put it in here.
//	void receiveByte(Bit8u data) {
//        receiveByteEx(data,0);
//    }
//
//	void receiveByteEx(Bit8u data, Bit8u error) {
//        #if SERIAL_DEBUG
//        log_ser(dbg_serialtraffic,data<0x10 ? "\t\t\t\trx 0x%02x (%u)":
//            "\t\t\t\trx 0x%02x (%c)", data, data);
//    #endif
//        if (!(rxfifo->addb(data))) {
//            // Overrun error ;o
//            error |= LSR_OVERRUN_ERROR_MASK;
//        }
//        removeEvent(SERIAL_RX_TIMEOUT_EVENT);
//        if(rxfifo->getUsage()==rx_interrupt_threshold) rise (RX_PRIORITY);
//        else setEvent(SERIAL_RX_TIMEOUT_EVENT,bytetime*4.0f);
//
//        if(error) {
//            // A lot of UART chips generate a framing error too when receiving break
//            if(error&LSR_RX_BREAK_MASK) error |= LSR_FRAMING_ERROR_MASK;
//    #if SERIAL_DEBUG
//            log_ser(dbg_serialtraffic,"with error: framing=%d,overrun=%d,break=%d,parity=%d",
//                (error&LSR_FRAMING_ERROR_MASK)>0,(error&LSR_OVERRUN_ERROR_MASK)>0,
//                (error&LSR_RX_BREAK_MASK)>0,(error&LSR_PARITY_ERROR_MASK)>0);
//    #endif
//            if(FCR&FCR_ACTIVATE) {
//                // error and FIFO active
//                if(!errorfifo->isFull()) {
//                    errors_in_fifo++;
//                    errorfifo->addb(error);
//                }
//                else {
//                    Bit8u toperror=errorfifo->getTop();
//                    if(!toperror) errors_in_fifo++;
//                    errorfifo->addb(error|toperror);
//                }
//                if(errorfifo->probeByte()) {
//                    // the next byte in the error fifo has an error
//                    rise (ERROR_PRIORITY);
//                    LSR |= error;
//                }
//            } else {
//                // error and FIFO inactive
//                rise (ERROR_PRIORITY);
//                LSR |= error;
//            };
//            if(error&LSR_PARITY_ERROR_MASK) {
//                parityErrors++;
//            };
//            if(error&LSR_OVERRUN_ERROR_MASK) {
//                overrunErrors++;
//                if(!GETFLAG(IF)) overrunIF0++;
//    #if SERIAL_DEBUG
//                log_ser(dbg_serialtraffic,"rx overrun (IF=%d)", GETFLAG(IF)>0);
//    #endif
//            };
//            if(error&LSR_FRAMING_ERROR_MASK) {
//                framingErrors++;
//            }
//            if(error&LSR_RX_BREAK_MASK) {
//                breakErrors++;
//            }
//            // trigger status window error notification
//            if(!errormsg_pending) {
//                errormsg_pending=true;
//                setEvent(SERIAL_ERRMSG_EVENT,1000);
//            }
//        } else {
//            // no error
//            if(FCR&FCR_ACTIVATE) {
//                errorfifo->addb(error);
//            }
//        }
//    }
//
//	// If an error was received, put it here (in LSR register format)
//	void receiveError(Bit8u errorword);
//
//	// depratched
//	// connected device checks, if port can receive data:
//	boolean CanReceiveByte() {
//        return !rxfifo->isFull();
//    }
//
//	// when THR was shifted to TX
//	void ByteTransmitting() {
//        if(sync_guardtime) {
//            //LOG_MSG("byte transmitting after guard");
//            //if(txfifo->isEmpty()) LOG_MSG("Serial port: FIFO empty when it should not");
//            sync_guardtime=false;
//            txfifo->getb();
//        } //else LOG_MSG("byte transmitting");
//        if(txfifo->isEmpty())rise (TX_PRIORITY);
//    }
//
//	// When done sending, notify here
//	void ByteTransmitted() {
//        if(!txfifo->isEmpty()) {
//            // there is more data
//            Bit8u data = txfifo->getb();
//    #if SERIAL_DEBUG
//            log_ser(dbg_serialtraffic,data<0x10?
//                "\t\t\t\t\ttx 0x%02x (%u) (from buffer)":
//                "\t\t\t\t\ttx 0x%02x (%c) (from buffer)",data,data);
//    #endif
//            if (loopback) setEvent(SERIAL_TX_LOOPBACK_EVENT, bytetime);
//            else transmitByte(data,false);
//            if(txfifo->isEmpty())rise (TX_PRIORITY);
//
//        } else {
//    #if SERIAL_DEBUG
//            log_ser(dbg_serialtraffic,"tx buffer empty.");
//    #endif
//            LSR |= LSR_TX_EMPTY_MASK;
//        }
//    }
//
//	// Transmit byte to prepherial
//	virtual void transmitByte(Bit8u val, boolean first)=0;
//
//	// switch break state to the passed value
//	virtual void setBreak(boolean value)=0;
//
//	// change baudrate, number of bits, parity, word length al at once
//	virtual void updatePortConfig(Bit16u divider, Bit8u lcr)=0;
//
//	void Init_Registers() {
//        // The "power on" settings
//        irq_active=false;
//        waiting_interrupts = 0x0;
//
//        Bit32u initbps = 9600;
//        Bit8u bytesize = 8;
//        char parity = 'N';
//
//        Bit8u lcrresult = 0;
//        Bit16u baudresult = 0;
//
//        IER = 0;
//        ISR = 0x1;
//        LCR = 0;
//        //MCR = 0xff;
//        loopback = true;
//        dtr=true;
//        rts=true;
//        op1=true;
//        op2=true;
//
//        sync_guardtime=false;
//        FCR=0xff;
//        Write_FCR(0x00);
//
//
//        LSR = 0x60;
//        d_cts = true;
//        d_dsr = true;
//        d_ri = true;
//        d_cd = true;
//        cts = true;
//        dsr = true;
//        ri = true;
//        cd = true;
//
//        SPR = 0xFF;
//
//        baud_divider=0x0;
//
//        // make lcr: byte size, parity, stopbits, baudrate
//
//        if (bytesize == 5)
//            lcrresult |= LCR_DATABITS_5;
//        else if (bytesize == 6)
//            lcrresult |= LCR_DATABITS_6;
//        else if (bytesize == 7)
//            lcrresult |= LCR_DATABITS_7;
//        else
//            lcrresult |= LCR_DATABITS_8;
//
//        switch(parity)
//        {
//        case 'N':
//        case 'n':
//            lcrresult |= LCR_PARITY_NONE;
//            break;
//        case 'O':
//        case 'o':
//            lcrresult |= LCR_PARITY_ODD;
//            break;
//        case 'E':
//        case 'e':
//            lcrresult |= LCR_PARITY_EVEN;
//            break;
//        case 'M':
//        case 'm':
//            lcrresult |= LCR_PARITY_MARK;
//            break;
//        case 'S':
//        case 's':
//            lcrresult |= LCR_PARITY_SPACE;
//            break;
//        }
//
//        // baudrate
//        if (initbps > 0)
//            baudresult = (Bit16u) (115200 / initbps);
//        else
//            baudresult = 12;			// = 9600 baud
//
//        Write_MCR (0);
//        Write_LCR (LCR_DIVISOR_Enable_MASK);
//        Write_THR ((Bit8u) baudresult & 0xff);
//        Write_IER ((Bit8u) (baudresult >> 8));
//        Write_LCR (lcrresult);
//        updateMSR();
//        Read_MSR();
//        PIC_DeActivateIRQ(irq);
//    }
//
//	boolean Putchar(Bit8u data, boolean wait_dtr, boolean wait_rts, /*Bitu*/int timeout) {
//        double starttime=PIC_FullIndex();
//        // wait for it to become empty
//        while(!(Read_LSR()&0x20)) {
//            CALLBACK_Idle();
//        }
//        // wait for DSR+CTS on
//        if(wait_dsr||wait_cts) {
//            if(wait_dsr||wait_cts) {
//                while(((Read_MSR()&0x30)!=0x30)&&(starttime>PIC_FullIndex()-timeout))
//                    CALLBACK_Idle();
//            } else if(wait_dsr) {
//                while(!(Read_MSR()&0x20)&&(starttime>PIC_FullIndex()-timeout))
//                    CALLBACK_Idle();
//            } else if(wait_cts) {
//                while(!(Read_MSR()&0x10)&&(starttime>PIC_FullIndex()-timeout))
//                    CALLBACK_Idle();
//            }
//            if(!(starttime>PIC_FullIndex()-timeout)) {
//    #if SERIAL_DEBUG
//                log_ser(dbg_aux,"Putchar timeout: MSR 0x%x",Read_MSR());
//    #endif
//                return false;
//            }
//        }
//        Write_THR(data);
//
//    #if SERIAL_DEBUG
//        log_ser(dbg_aux,"Putchar 0x%x",data);
//    #endif
//
//        return true;
//    }
//	boolean Getchar(Bit8u* data, Bit8u* lsr, boolean wait_dsr, /*Bitu*/int timeout) {
//        double starttime=PIC_FullIndex();
//        // wait for DSR on
//        if(wait_dsr) {
//            while((!(Read_MSR()&0x20))&&(starttime>PIC_FullIndex()-timeout))
//                CALLBACK_Idle();
//            if(!(starttime>PIC_FullIndex()-timeout)) {
//    #if SERIAL_DEBUG
//                log_ser(dbg_aux,"Getchar status timeout: MSR 0x%x",Read_MSR());
//    #endif
//                return false;
//            }
//        }
//        // wait for a byte to arrive
//        while((!((*lsr=Read_LSR())&0x1))&&(starttime>PIC_FullIndex()-timeout))
//            CALLBACK_Idle();
//
//        if(!(starttime>PIC_FullIndex()-timeout)) {
//    #if SERIAL_DEBUG
//            log_ser(dbg_aux,"Getchar data timeout: MSR 0x%x",Read_MSR());
//    #endif
//            return false;
//        }
//        *data=Read_RHR();
//
//    #if SERIAL_DEBUG
//        log_ser(dbg_aux,"Getchar read 0x%x",*data);
//    #endif
//        return true;
//    }
//
//
//private:
//
//	Dos_devices.DOS_Device* mydosdevice;
//
//	// I used this spec: st16c450v420.pdf
//
//	void ComputeInterrupts() {
//        /*Bitu*/int val = IER & waiting_interrupts;
//
//        if (val & ERROR_PRIORITY)			ISR = ISR_ERROR_VAL;
//        else if (val & TIMEOUT_PRIORITY)	ISR = ISR_FIFOTIMEOUT_VAL;
//        else if (val & RX_PRIORITY)			ISR = ISR_RX_VAL;
//        else if (val & TX_PRIORITY)			ISR = ISR_TX_VAL;
//        else if (val & MSR_PRIORITY)		ISR = ISR_MSR_VAL;
//        else ISR = ISR_CLEAR_VAL;
//
//        if(val && !irq_active)
//        {
//            irq_active=true;
//            if(op2) {
//                PIC_ActivateIRQ(irq);
//    #if SERIAL_DEBUG
//                log_ser(dbg_interrupt,"IRQ%d on.",irq);
//    #endif
//            }
//        } else if((!val) && irq_active) {
//            irq_active=false;
//            if(op2) {
//                PIC_DeActivateIRQ(irq);
//    #if SERIAL_DEBUG
//                log_ser(dbg_interrupt,"IRQ%d off.",irq);
//    #endif
//            }
//        }
//    }
//
//	// a sub-interrupt is triggered
//	void rise(Bit8u priority) {
//        #if SERIAL_DEBUG
//        if(priority&TX_PRIORITY && !(waiting_interrupts&TX_PRIORITY))
//            log_ser(dbg_interrupt,"tx interrupt on.");
//        if(priority&RX_PRIORITY && !(waiting_interrupts&RX_PRIORITY))
//            log_ser(dbg_interrupt,"rx interrupt on.");
//        if(priority&MSR_PRIORITY && !(waiting_interrupts&MSR_PRIORITY))
//            log_ser(dbg_interrupt,"msr interrupt on.");
//        if(priority&TIMEOUT_PRIORITY && !(waiting_interrupts&TIMEOUT_PRIORITY))
//            log_ser(dbg_interrupt,"fifo rx timeout interrupt on.");
//    #endif
//
//        waiting_interrupts |= priority;
//        ComputeInterrupts();
//    }
//
//	// clears the pending sub-interrupt
//	void clear(Bit8u priority) {
//        #if SERIAL_DEBUG
//        if(priority&TX_PRIORITY && (waiting_interrupts&TX_PRIORITY))
//            log_ser(dbg_interrupt,"tx interrupt off.");
//        if(priority&RX_PRIORITY && (waiting_interrupts&RX_PRIORITY))
//            log_ser(dbg_interrupt,"rx interrupt off.");
//        if(priority&MSR_PRIORITY && (waiting_interrupts&MSR_PRIORITY))
//            log_ser(dbg_interrupt,"msr interrupt off.");
//        if(priority&ERROR_PRIORITY && (waiting_interrupts&ERROR_PRIORITY))
//            log_ser(dbg_interrupt,"error interrupt off.");
//    #endif
//        waiting_interrupts &= (~priority);
//        ComputeInterrupts();
//    }
//
//	#define ERROR_PRIORITY 4	// overrun, parity error, frame error, break
//	#define RX_PRIORITY 1		// a byte has been received
//	#define TX_PRIORITY 2		// tx buffer has become empty
//	#define MSR_PRIORITY 8		// CRS, DSR, RI, DCD change
//	#define TIMEOUT_PRIORITY 0x10
//	#define NONE_PRIORITY 0
//
//	Bit8u waiting_interrupts;	// these are on, but maybe not enabled
//
//	// 16C550
//	//				read/write		name
//
//	Bit16u baud_divider;
//	#define RHR_OFFSET 0	// r Receive Holding Register, also LSB of Divisor Latch (r/w)
//							// Data: whole byte
//	#define THR_OFFSET 0	// w Transmit Holding Register
//							// Data: whole byte
//	Bit8u IER;	//	r/w		Interrupt Enable Register, also MSB of Divisor Latch
//	#define IER_OFFSET 1
//
//	boolean irq_active;
//
//	#define RHR_INT_Enable_MASK				0x1
//	#define THR_INT_Enable_MASK				0x2
//	#define Receive_Line_INT_Enable_MASK	0x4
//	#define Modem_Status_INT_Enable_MASK	0x8
//
//	Bit8u ISR;	//	r				Interrupt Status Register
//	#define ISR_OFFSET 2
//
//	#define ISR_CLEAR_VAL 0x1
//	#define ISR_FIFOTIMEOUT_VAL 0xc
//	#define ISR_ERROR_VAL 0x6
//	#define ISR_RX_VAL 0x4
//	#define ISR_TX_VAL 0x2
//	#define ISR_MSR_VAL 0x0
//public:
//	Bit8u LCR;	//	r/w				Line Control Register
//private:
//	#define LCR_OFFSET 3
//						// bit0: word length bit0
//						// bit1: word length bit1
//						// bit2: stop bits
//						// bit3: parity enable
//						// bit4: even parity
//						// bit5: set parity
//						// bit6: set break
//						// bit7: divisor latch enable
//
//
//	#define	LCR_BREAK_MASK 0x40
//	#define LCR_DIVISOR_Enable_MASK 0x80
//	#define LCR_PORTCONFIG_MASK 0x3F
//
//	#define LCR_PARITY_NONE		0x0
//	#define LCR_PARITY_ODD		0x8
//	#define LCR_PARITY_EVEN		0x18
//	#define LCR_PARITY_MARK		0x28
//	#define LCR_PARITY_SPACE	0x38
//
//	#define LCR_DATABITS_5		0x0
//	#define LCR_DATABITS_6		0x1
//	#define LCR_DATABITS_7		0x2
//	#define LCR_DATABITS_8		0x3
//
//	#define LCR_STOPBITS_1		0x0
//	#define LCR_STOPBITS_MORE_THAN_1 0x4
//
//	// Modem Control Register
//	// r/w
//	#define MCR_OFFSET 4
//	boolean dtr;			// bit0: DTR
//	boolean rts;			// bit1: RTS
//	boolean op1;			// bit2: OP1
//	boolean op2;			// bit3: OP2
//	boolean loopback;		// bit4: loop back enable
//
//	#define MCR_DTR_MASK 0x1
//	#define MCR_RTS_MASK 0x2
//	#define MCR_OP1_MASK 0x4
//	#define MCR_OP2_MASK 0x8
//	#define MCR_LOOPBACK_Enable_MASK 0x10
//public:
//	Bit8u LSR;	//	r				Line Status Register
//private:
//
//	#define LSR_OFFSET 5
//
//	#define LSR_RX_DATA_READY_MASK 0x1
//	#define LSR_OVERRUN_ERROR_MASK 0x2
//	#define LSR_PARITY_ERROR_MASK 0x4
//	#define LSR_FRAMING_ERROR_MASK 0x8
//	#define LSR_RX_BREAK_MASK 0x10
//	#define LSR_TX_HOLDING_EMPTY_MASK 0x20
//	#define LSR_TX_EMPTY_MASK 0x40
//
//	#define LSR_ERROR_MASK 0x1e
//
//	// error printing
//	boolean errormsg_pending;
//	/*Bitu*/int framingErrors;
//	/*Bitu*/int parityErrors;
//	/*Bitu*/int overrunErrors;
//	/*Bitu*/int txOverrunErrors;
//	/*Bitu*/int overrunIF0;
//	/*Bitu*/int breakErrors;
//
//
//	// Modem Status Register
//	//	r
//	#define MSR_OFFSET 6
//	boolean d_cts;			// bit0: deltaCTS
//	boolean d_dsr;			// bit1: deltaDSR
//	boolean d_ri;			// bit2: deltaRI
//	boolean d_cd;			// bit3: deltaCD
//	boolean cts;			// bit4: CTS
//	boolean dsr;			// bit5: DSR
//	boolean ri;			// bit6: RI
//	boolean cd;			// bit7: CD
//
//	#define MSR_delta_MASK 0xf
//	#define MSR_LINE_MASK 0xf0
//
//	#define MSR_dCTS_MASK 0x1
//	#define MSR_dDSR_MASK 0x2
//	#define MSR_dRI_MASK 0x4
//	#define MSR_dCD_MASK 0x8
//	#define MSR_CTS_MASK 0x10
//	#define MSR_DSR_MASK 0x20
//	#define MSR_RI_MASK 0x40
//	#define MSR_CD_MASK 0x80
//
//	Bit8u SPR;	//	r/w				Scratchpad Register
//	#define SPR_OFFSET 7
//
//
//	// For loopback purposes...
//	Bit8u loopback_data;
//	void transmitLoopbackByte(Bit8u val, boolean value);
//
//	// 16C550 (FIFO)
//	public: // todo remove
//	MyFifo* rxfifo;
//	private:
//	MyFifo* txfifo;
//	MyFifo* errorfifo;
//	/*Bitu*/int errors_in_fifo;
//	/*Bitu*/int rx_interrupt_threshold;
//	/*Bitu*/int fifosize;
//	Bit8u FCR;
//	boolean sync_guardtime;
//	#define FIFO_STATUS_ACTIVE 0xc0 // FIFO is active AND works ;)
//	#define FIFO_ERROR 0x80
//	#define FCR_ACTIVATE 0x01
//	#define FCR_CLEAR_RX 0x02
//	#define FCR_CLEAR_TX 0x04
//	#define FCR_OFFSET 2
//	#define FIFO_FLOWCONTROL 0x20
//};
//
//extern CSerial* serialports[];
//const Bit8u serial_defaultirq[] = { 4, 3, 4, 3 };
//const Bit16u serial_baseaddr[] = {0x3f8,0x2f8,0x3e8,0x2e8};
//const char* const serial_comname[]={"COM1","COM2","COM3","COM4"};
//
//    static /*Bitu*/int SERIAL_Read (/*Bitu*/int port, /*Bitu*/int iolen) {
//        /*Bitu*/int i;
//        /*Bitu*/int retval;
//        /*Bitu*/int index = port & 0x7;
//        switch(port&0xff8) {
//            case 0x3f8: i=0; break;
//            case 0x2f8: i=1; break;
//            case 0x3e8: i=2; break;
//            case 0x2e8: i=3; break;
//            default: return 0xff;
//        }
//        if(serialports[i]==0) return 0xff;
//
//        switch (index) {
//            case RHR_OFFSET:
//                retval = serialports[i]->Read_RHR();
//                break;
//            case IER_OFFSET:
//                retval = serialports[i]->Read_IER();
//                break;
//            case ISR_OFFSET:
//                retval = serialports[i]->Read_ISR();
//                break;
//            case LCR_OFFSET:
//                retval = serialports[i]->Read_LCR();
//                break;
//            case MCR_OFFSET:
//                retval = serialports[i]->Read_MCR();
//                break;
//            case LSR_OFFSET:
//                retval = serialports[i]->Read_LSR();
//                break;
//            case MSR_OFFSET:
//                retval = serialports[i]->Read_MSR();
//                break;
//            case SPR_OFFSET:
//                retval = serialports[i]->Read_SPR();
//                break;
//        }
//
//    #if SERIAL_DEBUG
//        const char* const dbgtext[]=
//            {"RHR","IER","ISR","LCR","MCR","LSR","MSR","SPR","DLL","DLM"};
//        if(serialports[i]->dbg_register) {
//            if((index<2) && ((serialports[i]->LCR)&LCR_DIVISOR_Enable_MASK))
//                index += 8;
//            serialports[i]->log_ser(serialports[i]->dbg_register,
//                "read  0x%2x from %s.",retval,dbgtext[index]);
//        }
//    #endif
//        return retval;
//    }
//    static void SERIAL_Write (/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int) {
//        /*Bitu*/int i;
//        /*Bitu*/int index = port & 0x7;
//        switch(port&0xff8) {
//            case 0x3f8: i=0; break;
//            case 0x2f8: i=1; break;
//            case 0x3e8: i=2; break;
//            case 0x2e8: i=3; break;
//            default: return;
//        }
//        if(serialports[i]==0) return;
//
//    #if SERIAL_DEBUG
//            const char* const dbgtext[]={"THR","IER","FCR",
//                "LCR","MCR","!LSR","MSR","SPR","DLL","DLM"};
//            if(serialports[i]->dbg_register) {
//                /*Bitu*/int debugindex=index;
//                if((index<2) && ((serialports[i]->LCR)&LCR_DIVISOR_Enable_MASK))
//                    debugindex += 8;
//                serialports[i]->log_ser(serialports[i]->dbg_register,
//                    "write 0x%2x to %s.",val,dbgtext[debugindex]);
//            }
//    #endif
//        switch (index) {
//            case THR_OFFSET:
//                serialports[i]->Write_THR (val);
//                return;
//            case IER_OFFSET:
//                serialports[i]->Write_IER (val);
//                return;
//            case FCR_OFFSET:
//                serialports[i]->Write_FCR (val);
//                return;
//            case LCR_OFFSET:
//                serialports[i]->Write_LCR (val);
//                return;
//            case MCR_OFFSET:
//                serialports[i]->Write_MCR (val);
//                return;
//            case MSR_OFFSET:
//                serialports[i]->Write_MSR (val);
//                return;
//            case SPR_OFFSET:
//                serialports[i]->Write_SPR (val);
//                return;
//            default:
//                serialports[i]->Write_reserved (val, port & 0x7);
//        }
//    }
//
//    static void Serial_EventHandler(/*Bitu*/int val) {
//        /*Bitu*/int serclassid=val&0x3;
//        if(serialports[serclassid]!=0)
//            serialports[serclassid]->handleEvent(val>>2);
//    }
}
